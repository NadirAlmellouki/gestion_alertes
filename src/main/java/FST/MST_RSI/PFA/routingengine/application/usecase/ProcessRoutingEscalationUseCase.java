package FST.MST_RSI.PFA.routingengine.application.usecase;

import FST.MST_RSI.PFA.notification.application.usecase.ExecuteNotificationWorkflowUseCase;
import FST.MST_RSI.PFA.routingengine.application.service.RoutingEscalationContextLoader;
import FST.MST_RSI.PFA.routingengine.domain.model.ResolvedPerson;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingDecision;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingExecutionStatus;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingPolicy;
import FST.MST_RSI.PFA.routingengine.domain.port.RoutingPolicyRepositoryPort;
import FST.MST_RSI.PFA.routingengine.domain.service.RoutingEscalationEngine;
import FST.MST_RSI.PFA.routingengine.infrastructure.config.RoutingEscalationProperties;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionEntity;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionRepository;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProcessRoutingEscalationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessRoutingEscalationUseCase.class);

    private final RoutingExecutionRepository routingExecutionRepository;
    private final RoutingPolicyRepositoryPort routingPolicyRepositoryPort;
    private final RoutingEscalationEngine routingEscalationEngine;
    private final RoutingEscalationContextLoader contextLoader;
    private final ExecuteNotificationWorkflowUseCase executeNotificationWorkflowUseCase;
    private final ScheduleRoutingEscalationUseCase scheduleRoutingEscalationUseCase;
    private final RoutingEscalationProperties properties;

    public ProcessRoutingEscalationUseCase(
            RoutingExecutionRepository routingExecutionRepository,
            RoutingPolicyRepositoryPort routingPolicyRepositoryPort,
            RoutingEscalationEngine routingEscalationEngine,
            RoutingEscalationContextLoader contextLoader,
            ExecuteNotificationWorkflowUseCase executeNotificationWorkflowUseCase,
            ScheduleRoutingEscalationUseCase scheduleRoutingEscalationUseCase,
            RoutingEscalationProperties properties
    ) {
        this.routingExecutionRepository = routingExecutionRepository;
        this.routingPolicyRepositoryPort = routingPolicyRepositoryPort;
        this.routingEscalationEngine = routingEscalationEngine;
        this.contextLoader = contextLoader;
        this.executeNotificationWorkflowUseCase = executeNotificationWorkflowUseCase;
        this.scheduleRoutingEscalationUseCase = scheduleRoutingEscalationUseCase;
        this.properties = properties;
    }

    @Transactional
    public void execute(UUID routingExecutionId) {
        RoutingExecutionEntity execution = routingExecutionRepository.findById(routingExecutionId).orElse(null);
        if (execution == null || !RoutingExecutionStatus.AWAITING_ESCALATION.equals(execution.getRoutingStatus())) {
            return;
        }

        if (isExpired(execution)) {
            routingEscalationEngine.expire(execution);
            return;
        }

        RoutingPolicy policy = routingPolicyRepositoryPort.findById(execution.getRoutingPolicyId()).orElse(null);
        if (policy == null) {
            routingEscalationEngine.complete(execution, "Routing policy not found");
            return;
        }

        RoutingEscalationContextLoader.LoadedEscalationContext loaded = contextLoader.load(execution);
        Optional<RoutingEscalationEngine.EscalationAdvanceResult> advance = routingEscalationEngine.advanceStep(
                execution,
                policy,
                loaded.routingContext()
        );

        if (advance.isEmpty()) {
            return;
        }

        RoutingEscalationEngine.EscalationAdvanceResult result = advance.get();
        if (RoutingExecutionStatus.NO_PERSON.equals(result.routingStatus())) {
            log.warn("Escalation stopped: no person for step {} on execution {}", result.step().stepOrder(), routingExecutionId);
            return;
        }

        ResolvedPerson selected = result.selectedPerson();
        RoutingDecision routingDecision = new RoutingDecision(
                execution.getId(),
                policy.id(),
                policy.code(),
                policy.origin(),
                selected.personId(),
                selected.email(),
                selected.fullName(),
                execution.getSelectedSolutionId(),
                result.step(),
                result.candidates(),
                RoutingExecutionStatus.STARTED
        );

        BusinessDecision businessDecision = BusinessDecision.continueRouting(
                execution.getSelectedSolutionId(),
                loaded.classification().matchedSolution(),
                result.step().targetRole()
        );

        ExecuteNotificationWorkflowUseCase.NotificationWorkflowResult notificationResult =
                executeNotificationWorkflowUseCase.execute(
                        new ExecuteNotificationWorkflowUseCase.NotificationWorkflowCommand(
                                loaded.alert(),
                                loaded.classification(),
                                businessDecision,
                                routingDecision
                        )
                );

        log.info("Escalation step {} for execution {}: notification={}",
                result.step().stepOrder(), routingExecutionId, notificationResult.outcome());

        scheduleRoutingEscalationUseCase.execute(execution.getId(), result.step().stepOrder());
    }

    private boolean isExpired(RoutingExecutionEntity execution) {
        Instant deadline = execution.getStartedAt().plus(properties.getMaxActiveMinutes(), ChronoUnit.MINUTES);
        return Instant.now().isAfter(deadline);
    }
}
