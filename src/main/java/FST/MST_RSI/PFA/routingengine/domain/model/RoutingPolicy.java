package FST.MST_RSI.PFA.routingengine.domain.model;

import java.util.List;
import java.util.UUID;

public record RoutingPolicy(
        UUID id,
        String code,
        String name,
        String description,
        boolean enabled,
        int priority,
        PolicyOrigin origin,
        List<RoutingStepDefinition> steps
) {
    public RoutingPolicy {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
