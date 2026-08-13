package FST.MST_RSI.PFA.routingengine.domain.model;

import java.util.List;
import java.util.UUID;

public record RoutingDecision(
        UUID routingExecutionId,
        UUID policyId,
        String policyCode,
        PolicyOrigin policyOrigin,
        UUID selectedPersonId,
        String selectedPersonEmail,
        String selectedPersonName,
        UUID selectedSolutionId,
        RoutingStepDefinition currentStep,
        List<ResolvedPerson> candidatePersons,
        String routingStatus
) {
    public RoutingDecision {
        candidatePersons = candidatePersons == null ? List.of() : List.copyOf(candidatePersons);
    }
}
