package FST.MST_RSI.PFA.audit.application.usecase;

import FST.MST_RSI.PFA.audit.domain.model.AuditAction;
import FST.MST_RSI.PFA.audit.domain.model.AuditRecord;
import FST.MST_RSI.PFA.audit.domain.model.SystemEventRecord;
import FST.MST_RSI.PFA.audit.domain.model.SystemEventSeverity;
import FST.MST_RSI.PFA.audit.domain.port.AuditRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordAuditEventUseCaseTest {

    @Mock
    private AuditRepositoryPort auditRepositoryPort;

    @Test
    void persistsAuditRecord() {
        UUID auditId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        when(auditRepositoryPort.saveAudit(org.mockito.ArgumentMatchers.any())).thenReturn(auditId);

        RecordAuditEventUseCase useCase = new RecordAuditEventUseCase(auditRepositoryPort);
        UUID result = useCase.execute(new AuditRecord(
                AuditAction.ALERT_RECEIVED,
                alertId,
                null,
                null,
                null,
                null,
                "Alert",
                alertId,
                "Alerte reçue",
                alertId.toString(),
                null,
                List.of()
        ));

        assertThat(result).isEqualTo(auditId);
        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRepositoryPort).saveAudit(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo(AuditAction.ALERT_RECEIVED);
        assertThat(captor.getValue().alertId()).isEqualTo(alertId);
    }

    @Test
    void persistsSystemEvent() {
        UUID eventId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        when(auditRepositoryPort.saveSystemEvent(org.mockito.ArgumentMatchers.any())).thenReturn(eventId);

        RecordAuditEventUseCase useCase = new RecordAuditEventUseCase(auditRepositoryPort);
        UUID result = useCase.executeSystemEvent(new SystemEventRecord(
                alertId,
                null,
                "monitoring",
                SystemEventSeverity.WARN,
                "DYNATRACE_API_UNAVAILABLE",
                "API indisponible",
                alertId.toString()
        ));

        assertThat(result).isEqualTo(eventId);
        ArgumentCaptor<SystemEventRecord> captor = ArgumentCaptor.forClass(SystemEventRecord.class);
        verify(auditRepositoryPort).saveSystemEvent(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("DYNATRACE_API_UNAVAILABLE");
    }
}
