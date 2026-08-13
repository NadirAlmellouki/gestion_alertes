package FST.MST_RSI.PFA.routingengine.domain.model;

import java.util.UUID;

public record RoutingStepDefinition(
        UUID id,
        int stepOrder,
        String actionType,
        String targetRole,
        String targetUnitType,
        String channel,
        int delayAfterSeconds
) {
}
