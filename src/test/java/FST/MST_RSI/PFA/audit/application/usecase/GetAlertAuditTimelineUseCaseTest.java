package FST.MST_RSI.PFA.audit.application.usecase;

import FST.MST_RSI.PFA.audit.domain.model.AuditAction;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.AuditLogEntity;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.AuditLogDetailJpaRepository;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.AuditLogJpaRepository;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.SystemEventEntity;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.SystemEventJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAlertAuditTimelineUseCaseTest {

    @Mock
    private AuditLogJpaRepository auditLogJpaRepository;
    @Mock
    private AuditLogDetailJpaRepository auditLogDetailJpaRepository;
    @Mock
    private SystemEventJpaRepository systemEventJpaRepository;

    @Test
    void mergesAuditLogsAndSystemEventsChronologically() {
        UUID alertId = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-08-18T10:00:00Z");
        Instant t2 = Instant.parse("2026-08-18T10:05:00Z");

        AuditLogEntity audit = AuditLogEntity.create(
                UUID.randomUUID(), null, alertId, null, null, null,
                AuditAction.ALERT_RECEIVED, "Alert", alertId,
                "Alerte reçue", alertId.toString(), null, t1
        );
        SystemEventEntity systemEvent = SystemEventEntity.create(
                UUID.randomUUID(), alertId, null, "monitoring", "WARN",
                "API_ERROR", "Dynatrace indisponible", alertId.toString(), t2
        );

        when(auditLogJpaRepository.findByAlertIdOrderByCreatedAtAsc(alertId)).thenReturn(List.of(audit));
        when(auditLogDetailJpaRepository.findByAuditLogIdOrderByFieldNameAsc(audit.getId())).thenReturn(List.of());
        when(systemEventJpaRepository.findByAlertIdOrderByCreatedAtAsc(alertId)).thenReturn(List.of(systemEvent));

        GetAlertAuditTimelineUseCase useCase = new GetAlertAuditTimelineUseCase(
                auditLogJpaRepository,
                auditLogDetailJpaRepository,
                systemEventJpaRepository
        );

        var timeline = useCase.execute(alertId.toString());

        assertThat(timeline).hasSize(2);
        assertThat(timeline.get(0).entryType()).isEqualTo("AUDIT");
        assertThat(timeline.get(0).actionOrEventType()).isEqualTo(AuditAction.ALERT_RECEIVED);
        assertThat(timeline.get(1).entryType()).isEqualTo("SYSTEM");
        assertThat(timeline.get(1).severity()).isEqualTo("WARN");
    }
}
