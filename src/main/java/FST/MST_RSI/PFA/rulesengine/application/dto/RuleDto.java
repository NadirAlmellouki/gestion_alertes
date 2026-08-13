package FST.MST_RSI.PFA.rulesengine.application.dto;

import FST.MST_RSI.PFA.rulesengine.domain.model.ConditionBlockType;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleOrigin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record RuleDto(
        UUID id,
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotNull Integer evaluationPriority,
        boolean enabled,
        boolean stopOnMatch,
        RuleOrigin origin,
        @Valid @NotNull List<RuleConditionGroupDto> conditionGroups,
        @Valid @NotNull List<RuleActionDto> actions
) {
    public record RuleConditionGroupDto(
            UUID id,
            @NotNull ConditionBlockType blockType,
            @NotBlank String logicalOperator,
            int executionOrder,
            @Valid @NotNull List<RuleConditionDto> conditions
    ) {
    }

    public record RuleConditionDto(
            UUID id,
            @NotBlank String fieldName,
            @NotBlank String operator,
            String expectedValue,
            String valueType,
            int conditionOrder
    ) {
    }

    public record RuleActionDto(
            UUID id,
            @NotBlank String actionType,
            String actionValue,
            int executionOrder
    ) {
    }
}
