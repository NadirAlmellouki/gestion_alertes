package FST.MST_RSI.PFA.routingengine.application.usecase;

import FST.MST_RSI.PFA.audit.application.service.AuditRecorder;
import FST.MST_RSI.PFA.audit.domain.model.AuditAction;
import FST.MST_RSI.PFA.audit.domain.model.AuditRecord;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingExecutionStatus;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingPolicy;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingStepDefinition;
import FST.MST_RSI.PFA.routingengine.domain.port.RoutingPolicyRepositoryPort;
import FST.MST_RSI.PFA.routingengine.domain.service.RoutingEscalationEngine;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionEntity;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ScheduleRoutingEscalationUseCase {

    private final RoutingExecutionRepository routingExecutionRepository;
    private final RoutingPolicyRepositoryPort routingPolicyRepositoryPort;
    private final RoutingEscalationEngine routingEscalationEngine;
    private final AuditRecorder auditRecorder;

    public ScheduleRoutingEscalationUseCase(
            RoutingExecutionRepository routingExecutionRepository,
            RoutingPolicyRepositoryPort routingPolicyRepositoryPort,
            RoutingEscalationEngine routingEscalationEngine,
            AuditRecorder auditRecorder
    ) {
        this.routingExecutionRepository = routingExecutionRepository;
        this.routingPolicyRepositoryPort = routingPolicyRepositoryPort;
        this.routingEscalationEngine = routingEscalationEngine;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public void execute(UUID routingExecutionId, int currentStepOrder) {
        RoutingExecutionEntity execution = routingExecutionRepository.findById(routingExecutionId)
                .orElse(null);
        if (execution == null || execution.getRoutingPolicyId() == null) {
            return;
        }
        if (RoutingExecutionStatus.COMPLETED.equals(execution.getRoutingStatus())
                || RoutingExecutionStatus.EXPIRED.equals(execution.getRoutingStatus())) {
            return;
        }

        RoutingPolicy policy = routingPolicyRepositoryPort.findById(execution.getRoutingPolicyId()).orElse(null);
        if (policy == null) {
            return;
        }

        RoutingStepDefinition currentStep = policy.steps().stream()
                .filter(s -> s.stepOrder() == currentStepOrder)
                .findFirst()
                .orElse(null);
        if (currentStep == null) {
            return;
        }

        boolean hasNext = policy.steps().stream().anyMatch(s -> s.stepOrder() > currentStepOrder);
        if (!hasNext) {
            routingEscalationEngine.complete(execution, "Final routing step reached");
            return;
        }

        routingEscalationEngine.scheduleNextStep(execution, currentStep);
        auditRecorder.record(new AuditRecord(
                AuditAction.ESCALATION_SCHEDULED,
                execution.getAlertId(),
                execution.getClassificationId(),
                execution.getId(),
                null,
                null,
                "RoutingExecution",
                execution.getId(),
                "Escalation planifiée après l'étape " + currentStepOrder
                        + " (action=" + currentStep.actionType() + ", delay=" + currentStep.delayAfterSeconds() + "s)",
                AuditRecorder.correlationId(execution.getAlertId()),
                null,
                List.of()
        ));
    }
}
