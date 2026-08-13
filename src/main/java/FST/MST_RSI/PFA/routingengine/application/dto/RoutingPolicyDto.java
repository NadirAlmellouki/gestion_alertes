package FST.MST_RSI.PFA.routingengine.application.dto;

import FST.MST_RSI.PFA.routingengine.domain.model.PolicyOrigin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record RoutingPolicyDto(
        UUID id,
        @NotBlank String code,
        @NotBlank String name,
        String description,
        boolean enabled,
        @NotNull Integer priority,
        PolicyOrigin origin,
        @Valid @NotNull List<RoutingStepDto> steps
) {
    public record RoutingStepDto(
            UUID id,
            int stepOrder,
            @NotBlank String actionType,
            @NotBlank String targetRole,
            @NotBlank String targetUnitType,
            String channel,
            int delayAfterSeconds
    ) {
    }
}
