package FST.MST_RSI.PFA.routingengine.domain.service;

import FST.MST_RSI.PFA.routingengine.domain.model.ResolvedPerson;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingContext;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingExecutionStatus;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingPolicy;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingStepDefinition;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionEntity;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingHistoryEntity;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingHistoryRepository;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoutingEscalationEngine {

    private final PersonResolver personResolver;
    private final RoutingExecutionRepository routingExecutionRepository;
    private final RoutingHistoryRepository routingHistoryRepository;

    public RoutingEscalationEngine(
            PersonResolver personResolver,
            RoutingExecutionRepository routingExecutionRepository,
            RoutingHistoryRepository routingHistoryRepository
    ) {
        this.personResolver = personResolver;
        this.routingExecutionRepository = routingExecutionRepository;
        this.routingHistoryRepository = routingHistoryRepository;
    }

    @Transactional
    public void scheduleNextStep(RoutingExecutionEntity execution, RoutingStepDefinition currentStep) {
        if (execution.getRoutingStatus().equals(RoutingExecutionStatus.COMPLETED)) {
            return;
        }
        int delaySeconds = Math.max(0, currentStep.delayAfterSeconds());
        Instant nextAt = Instant.now().plusSeconds(delaySeconds);
        execution.setNextEscalationAt(nextAt);
        execution.setRoutingStatus(RoutingExecutionStatus.AWAITING_ESCALATION);
        routingExecutionRepository.save(execution);
        recordHistory(execution, currentStep, execution.getSelectedPersonId(), null,
                "SCHEDULED", "Next step in " + delaySeconds + "s");
    }

    @Transactional
    public Optional<EscalationAdvanceResult> advanceStep(
            RoutingExecutionEntity execution,
            RoutingPolicy policy,
            RoutingContext context
    ) {
        execution.setRoutingStatus(RoutingExecutionStatus.IN_PROGRESS);
        execution.setNextEscalationAt(null);

        RoutingStepDefinition currentStepDef = findStepByOrder(policy, execution.getCurrentStep())
                .orElse(null);
        RoutingStepDefinition nextStep = findNextStep(policy, execution.getCurrentStep()).orElse(null);

        if (nextStep == null) {
            complete(execution, "No further routing steps");
            return Optional.empty();
        }

        int candidateIndex = execution.getCandidateIndex();
        if (isNextPersonAction(nextStep.actionType())) {
            candidateIndex = execution.getCandidateIndex() + 1;
        } else if (isRetryAction(nextStep.actionType()) && currentStepDef != null) {
            // keep same candidate index for retry
        }

        List<ResolvedPerson> candidates = personResolver.resolve(context, nextStep);
        ResolvedPerson selected = selectCandidate(candidates, candidateIndex);

        execution.setCurrentStep(nextStep.stepOrder());
        execution.setCandidateIndex(candidateIndex);

        if (selected == null) {
            execution.setSelectedPersonId(null);
            execution.setRoutingStatus(RoutingExecutionStatus.NO_PERSON);
            execution.setFinishedAt(Instant.now());
            routingExecutionRepository.save(execution);
            recordHistory(execution, nextStep, null, resolveUnitId(context, nextStep),
                    nextStep.actionType(), "No person available");
            return Optional.of(new EscalationAdvanceResult(nextStep, null, candidates, RoutingExecutionStatus.NO_PERSON));
        }

        execution.setSelectedPersonId(selected.personId());
        execution.setRoutingStatus(RoutingExecutionStatus.STARTED);
        routingExecutionRepository.save(execution);
        recordHistory(execution, nextStep, selected.personId(), selected.unitId(),
                nextStep.actionType(), "Escalated to " + selected.fullName());

        return Optional.of(new EscalationAdvanceResult(nextStep, selected, candidates, RoutingExecutionStatus.STARTED));
    }

    @Transactional
    public void complete(RoutingExecutionEntity execution, String reason) {
        execution.setRoutingStatus(RoutingExecutionStatus.COMPLETED);
        execution.setNextEscalationAt(null);
        execution.setFinishedAt(Instant.now());
        routingExecutionRepository.save(execution);
        recordHistory(execution, null, execution.getSelectedPersonId(), null,
                RoutingExecutionStatus.COMPLETED, reason);
    }

    @Transactional
    public void expire(RoutingExecutionEntity execution) {
        execution.setRoutingStatus(RoutingExecutionStatus.EXPIRED);
        execution.setNextEscalationAt(null);
        execution.setFinishedAt(Instant.now());
        routingExecutionRepository.save(execution);
        recordHistory(execution, null, execution.getSelectedPersonId(), null,
                RoutingExecutionStatus.EXPIRED, "Max escalation duration reached");
    }

    private Optional<RoutingStepDefinition> findStepByOrder(RoutingPolicy policy, int stepOrder) {
        return policy.steps().stream()
                .filter(s -> s.stepOrder() == stepOrder)
                .findFirst();
    }

    private Optional<RoutingStepDefinition> findNextStep(RoutingPolicy policy, int currentStepOrder) {
        return policy.steps().stream()
                .filter(s -> s.stepOrder() > currentStepOrder)
                .findFirst();
    }

    private static ResolvedPerson selectCandidate(List<ResolvedPerson> candidates, int index) {
        if (candidates.isEmpty() || index >= candidates.size()) {
            return null;
        }
        return candidates.get(index);
    }

    private static boolean isNextPersonAction(String actionType) {
        return "NEXT_PERSON".equalsIgnoreCase(actionType);
    }

    private static boolean isRetryAction(String actionType) {
        return "VOICE_RETRY".equalsIgnoreCase(actionType) || "VOICE_CALL".equalsIgnoreCase(actionType);
    }

    private static UUID resolveUnitId(RoutingContext context, RoutingStepDefinition step) {
        return switch (step.targetUnitType()) {
            case "SOLUTION" -> context.solutionUnitId();
            case "DOMAIN" -> context.domainUnitId();
            case "POLE" -> context.poleUnitId();
            case "ENTITY" -> context.entityUnitId();
            default -> null;
        };
    }

    private void recordHistory(
            RoutingExecutionEntity execution,
            RoutingStepDefinition step,
            UUID personId,
            UUID unitId,
            String action,
            String details
    ) {
        routingHistoryRepository.save(RoutingHistoryEntity.create(
                UUID.randomUUID(),
                execution.getId(),
                step == null ? null : step.id(),
                personId,
                unitId,
                action,
                details,
                Instant.now()
        ));
    }

    public record EscalationAdvanceResult(
            RoutingStepDefinition step,
            ResolvedPerson selectedPerson,
            List<ResolvedPerson> candidates,
            String routingStatus
    ) {
    }
}
