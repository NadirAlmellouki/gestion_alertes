package FST.MST_RSI.PFA.routingengine.application.usecase;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.audit.application.service.AuditRecorder;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.notification.application.usecase.ExecuteNotificationWorkflowUseCase;
import FST.MST_RSI.PFA.routingengine.application.service.RoutingEscalationContextLoader;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingContext;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingExecutionStatus;
import FST.MST_RSI.PFA.routingengine.domain.port.RoutingPolicyRepositoryPort;
import FST.MST_RSI.PFA.routingengine.domain.service.EscalationConditionEvaluator;
import FST.MST_RSI.PFA.routingengine.domain.service.RoutingEscalationEngine;
import FST.MST_RSI.PFA.routingengine.infrastructure.config.RoutingEscalationProperties;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionEntity;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessRoutingEscalationUseCaseTest {

    @Mock
    private RoutingExecutionRepository routingExecutionRepository;
    @Mock
    private RoutingPolicyRepositoryPort routingPolicyRepositoryPort;
    @Mock
    private RoutingEscalationEngine routingEscalationEngine;
    @Mock
    private EscalationConditionEvaluator escalationConditionEvaluator;
    @Mock
    private RoutingEscalationContextLoader contextLoader;
    @Mock
    private ExecuteNotificationWorkflowUseCase executeNotificationWorkflowUseCase;
    @Mock
    private ScheduleRoutingEscalationUseCase scheduleRoutingEscalationUseCase;
    @Mock
    private AuditRecorder auditRecorder;

    private ProcessRoutingEscalationUseCase useCase;
    private final UUID executionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        RoutingEscalationProperties properties = new RoutingEscalationProperties();
        properties.setMaxActiveMinutes(24 * 60);
        useCase = new ProcessRoutingEscalationUseCase(
                routingExecutionRepository,
                routingPolicyRepositoryPort,
                routingEscalationEngine,
                escalationConditionEvaluator,
                contextLoader,
                executeNotificationWorkflowUseCase,
                scheduleRoutingEscalationUseCase,
                properties,
                auditRecorder
        );
    }

    @Test
    void doesNotAdvanceWhenVoipAlreadyAnswered() {
        RoutingExecutionEntity execution = awaiting();
        Alert alert = openAlert();
        when(routingExecutionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        when(contextLoader.load(execution)).thenReturn(loaded(alert));
        when(escalationConditionEvaluator.stopReason(executionId, alert))
                .thenReturn(Optional.of("Appel VoIP répondu — prise en charge, escalade arrêtée"));

        useCase.execute(executionId);

        verify(routingEscalationEngine).complete(execution, "Appel VoIP répondu — prise en charge, escalade arrêtée");
        verify(routingEscalationEngine, never()).advanceStep(any(), any(), any());
        verify(executeNotificationWorkflowUseCase, never()).execute(any());
    }

    @Test
    void doesNotAdvanceWhenIncidentResolved() {
        RoutingExecutionEntity execution = awaiting();
        Alert alert = openAlert();
        when(routingExecutionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        when(contextLoader.load(execution)).thenReturn(loaded(alert));
        when(escalationConditionEvaluator.stopReason(executionId, alert))
                .thenReturn(Optional.of("Incident Dynatrace résolu — escalade arrêtée"));

        useCase.execute(executionId);

        verify(routingEscalationEngine).complete(execution, "Incident Dynatrace résolu — escalade arrêtée");
        verify(routingEscalationEngine, never()).advanceStep(any(), any(), any());
    }

    @Test
    void advancesWhenConditionStillTrue() {
        RoutingExecutionEntity execution = awaiting();
        Alert alert = openAlert();
        when(routingExecutionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        when(contextLoader.load(execution)).thenReturn(loaded(alert));
        when(escalationConditionEvaluator.stopReason(executionId, alert)).thenReturn(Optional.empty());
        when(routingPolicyRepositoryPort.findById(execution.getRoutingPolicyId())).thenReturn(Optional.empty());

        useCase.execute(executionId);

        verify(routingEscalationEngine).complete(execution, "Routing policy not found");
        verify(executeNotificationWorkflowUseCase, never()).execute(any());
    }

    private RoutingExecutionEntity awaiting() {
        return RoutingExecutionEntity.create(
                executionId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                RoutingExecutionStatus.AWAITING_ESCALATION,
                Instant.now().minusSeconds(60)
        );
    }

    private static Alert openAlert() {
        return Alert.createNew(
                "P-1", "t", "app", "PROD", "HIGH", "APPLICATION",
                "OPEN", "u", "h", "{}", Instant.now()
        );
    }

    private RoutingEscalationContextLoader.LoadedEscalationContext loaded(Alert alert) {
        return new RoutingEscalationContextLoader.LoadedEscalationContext(
                alert,
                new ClassificationResult(
                        null, null, null, null, null, null, null, null, null, null,
                        java.util.List.of(), false, null, null, null
                ),
                new RoutingContext(
                        alert.getId().value(), null, null, null, null, null, null, null, null
                )
        );
    }
}
