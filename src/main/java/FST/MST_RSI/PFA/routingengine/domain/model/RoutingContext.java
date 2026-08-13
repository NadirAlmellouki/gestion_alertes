package FST.MST_RSI.PFA.routingengine.domain.model;

import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessDecision;

import java.util.UUID;

public record RoutingContext(
        UUID alertId,
        UUID llmAnalysisId,
        BusinessDecision businessDecision,
        UUID solutionUnitId,
        String solutionName,
        UUID domainUnitId,
        UUID poleUnitId,
        UUID entityUnitId,
        String forcedRole
) {
}
