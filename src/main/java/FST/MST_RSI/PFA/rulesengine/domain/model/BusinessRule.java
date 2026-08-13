package FST.MST_RSI.PFA.rulesengine.domain.model;

import java.util.List;
import java.util.UUID;

public record BusinessRule(
        UUID id,
        String code,
        String name,
        String description,
        int evaluationPriority,
        boolean enabled,
        boolean stopOnMatch,
        RuleOrigin origin,
        List<RuleConditionGroup> conditionGroups,
        List<RuleAction> actions
) {
    public BusinessRule {
        conditionGroups = conditionGroups == null ? List.of() : List.copyOf(conditionGroups);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public List<RuleConditionGroup> conditionBlocks() {
        return conditionGroups.stream()
                .filter(g -> g.blockType() == ConditionBlockType.CONDITION)
                .toList();
    }

    public List<RuleConditionGroup> exceptionBlocks() {
        return conditionGroups.stream()
                .filter(g -> g.blockType() == ConditionBlockType.EXCEPTION)
                .toList();
    }
}
