package FST.MST_RSI.PFA.monitoring.application.usecase;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.audit.application.service.AuditRecorder;
import FST.MST_RSI.PFA.monitoring.domain.model.DynatraceProblemSnapshot;
import FST.MST_RSI.PFA.monitoring.domain.model.ResolutionCheckStatus;
import FST.MST_RSI.PFA.monitoring.domain.port.DynatraceProblemPort;
import FST.MST_RSI.PFA.monitoring.infrastructure.config.ResolutionCheckProperties;
import FST.MST_RSI.PFA.monitoring.infrastructure.persistence.ResolutionCheckEntity;
import FST.MST_RSI.PFA.monitoring.infrastructure.persistence.ResolutionCheckRepository;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingExecutionStatus;
import FST.MST_RSI.PFA.routingengine.domain.service.RoutingEscalationEngine;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionEntity;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessResolutionCheckUseCaseTest {

    @Mock
    private ResolutionCheckRepository resolutionCheckRepository;
    @Mock
    private DynatraceProblemPort dynatraceProblemPort;
    @Mock
    private AlertRepositoryPort alertRepositoryPort;
    @Mock
    private RoutingExecutionRepository routingExecutionRepository;
    @Mock
    private RoutingEscalationEngine routingEscalationEngine;
    @Mock
    private AuditRecorder auditRecorder;

    private ResolutionCheckProperties properties;
    private ProcessResolutionCheckUseCase useCase;

    private final UUID checkId = UUID.randomUUID();
    private final UUID alertId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        properties = new ResolutionCheckProperties();
        properties.setMaxAttempts(10);
        properties.setMaxDurationMinutes(60);
        properties.setPollingIntervalSeconds(60);
        useCase = new ProcessResolutionCheckUseCase(
                resolutionCheckRepository,
                dynatraceProblemPort,
                alertRepositoryPort,
                routingExecutionRepository,
                routingEscalationEngine,
                properties,
                auditRecorder
        );
        lenient().when(resolutionCheckRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void marksCheckResolvedWhenDynatraceProblemClosed() {
        ResolutionCheckEntity check = activeCheck();
        Alert alert = sampleAlert();
        RoutingExecutionEntity execution = routingExecution();

        when(resolutionCheckRepository.findById(checkId)).thenReturn(Optional.of(check));
        when(dynatraceProblemPort.fetchProblem("P-123"))
                .thenReturn(Optional.of(new DynatraceProblemSnapshot("P-123", "RESOLVED")));
        when(alertRepositoryPort.findById(AlertId.of(alertId.toString()))).thenReturn(Optional.of(alert));
        when(routingExecutionRepository.findActiveByAlertId(alertId)).thenReturn(List.of(execution));

        useCase.execute(checkId);

        assertThat(check.getStatus()).isEqualTo(ResolutionCheckStatus.RESOLVED);
        assertThat(check.getFinishedAt()).isNotNull();
        assertThat(alert.getDynatraceState()).isEqualTo("RESOLVED");
        verify(routingEscalationEngine).complete(execution, "Dynatrace problem resolved");
    }

    @Test
    void schedulesNextAttemptWhenProblemStillOpen() {
        ResolutionCheckEntity check = activeCheck();
        when(resolutionCheckRepository.findById(checkId)).thenReturn(Optional.of(check));
        when(dynatraceProblemPort.fetchProblem("P-123"))
                .thenReturn(Optional.of(new DynatraceProblemSnapshot("P-123", "OPEN")));

        useCase.execute(checkId);

        assertThat(check.getStatus()).isEqualTo(ResolutionCheckStatus.ACTIVE);
        assertThat(check.getAttemptCount()).isEqualTo(1);
        assertThat(check.getNextCheckAt()).isNotNull();
        assertThat(check.getLastDynatraceState()).isEqualTo("OPEN");
    }

    @Test
    void expiresWhenMaxAttemptsReached() {
        ResolutionCheckEntity check = activeCheck();
        check.setAttemptCount(10);
        when(resolutionCheckRepository.findById(checkId)).thenReturn(Optional.of(check));

        useCase.execute(checkId);

        assertThat(check.getStatus()).isEqualTo(ResolutionCheckStatus.EXPIRED);
        assertThat(check.getFinishedAt()).isNotNull();
    }

    private ResolutionCheckEntity activeCheck() {
        return ResolutionCheckEntity.create(
                checkId,
                alertId,
                "P-123",
                ResolutionCheckStatus.ACTIVE,
                Instant.now().minusSeconds(5),
                Instant.now().minusSeconds(300)
        );
    }

    private static Alert sampleAlert() {
        return Alert.createNew(
                "P-123", "CPU saturation", "PayCore", "PROD", "CRITICAL", "APPLICATION",
                "OPEN", "http://dynatrace/problem/1", "host-1", "{}", Instant.now()
        );
    }

    private RoutingExecutionEntity routingExecution() {
        return RoutingExecutionEntity.create(
                UUID.randomUUID(),
                alertId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                RoutingExecutionStatus.AWAITING_ESCALATION,
                Instant.now()
        );
    }
}
