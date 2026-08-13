package FST.MST_RSI.PFA.routingengine.domain.service;

import FST.MST_RSI.PFA.routingengine.domain.model.PolicyOrigin;
import FST.MST_RSI.PFA.routingengine.domain.model.ResolvedPerson;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingContext;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingDecision;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingPolicy;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingStepDefinition;
import FST.MST_RSI.PFA.routingengine.domain.port.RoutingPolicyRepositoryPort;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionEntity;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RoutingEngine {

    private final RoutingPolicyRepositoryPort policyRepositoryPort;
    private final PersonResolver personResolver;
    private final RoutingExecutionRepository routingExecutionRepository;

    public RoutingEngine(
            RoutingPolicyRepositoryPort policyRepositoryPort,
            PersonResolver personResolver,
            RoutingExecutionRepository routingExecutionRepository
    ) {
        this.policyRepositoryPort = policyRepositoryPort;
        this.personResolver = personResolver;
        this.routingExecutionRepository = routingExecutionRepository;
    }

    public RoutingDecision buildRoutingDecision(RoutingContext context) {
        if (context.businessDecision().humanValidationRequired()) {
            return pendingDecision(null, null, "AWAITING_HUMAN_VALIDATION", List.of());
        }
        if (!context.businessDecision().routingTriggered()) {
            return pendingDecision(null, null, "SKIPPED", List.of());
        }

        RoutingPolicy policy = selectPolicy();
        if (policy == null || policy.steps().isEmpty()) {
            return pendingDecision(null, null, "NO_POLICY", List.of());
        }

        RoutingStepDefinition firstStep = policy.steps().getFirst();
        List<ResolvedPerson> candidates = personResolver.resolve(context, firstStep);
        ResolvedPerson selected = candidates.isEmpty() ? null : candidates.getFirst();

        UUID executionId = UUID.randomUUID();
        routingExecutionRepository.save(RoutingExecutionEntity.create(
                executionId,
                context.alertId(),
                context.llmAnalysisId(),
                policy.id(),
                context.solutionUnitId(),
                selected == null ? null : selected.personId(),
                firstStep.stepOrder(),
                selected == null ? "NO_PERSON" : "STARTED",
                Instant.now()
        ));

        return new RoutingDecision(
                executionId,
                policy.id(),
                policy.code(),
                policy.origin(),
                selected == null ? null : selected.personId(),
                selected == null ? null : selected.email(),
                selected == null ? null : selected.fullName(),
                context.solutionUnitId(),
                firstStep,
                candidates,
                selected == null ? "NO_PERSON" : "STARTED"
        );
    }

    private RoutingPolicy selectPolicy() {
        List<RoutingPolicy> configured = policyRepositoryPort.findEnabledByOrigin(PolicyOrigin.CONFIGURED);
        if (!configured.isEmpty()) {
            return configured.getFirst();
        }
        List<RoutingPolicy> defaults = policyRepositoryPort.findEnabledByOrigin(PolicyOrigin.DEFAULT);
        return defaults.isEmpty() ? null : defaults.getFirst();
    }

    private RoutingDecision pendingDecision(UUID policyId, String policyCode, String status, List<ResolvedPerson> candidates) {
        return new RoutingDecision(null, policyId, policyCode, null, null, null, null, null, null, candidates, status);
    }
}
