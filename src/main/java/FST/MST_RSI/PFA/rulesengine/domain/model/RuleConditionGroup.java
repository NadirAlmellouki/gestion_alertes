package FST.MST_RSI.PFA.rulesengine.domain.model;

import java.util.List;
import java.util.UUID;

public record RuleConditionGroup(
        UUID id,
        ConditionBlockType blockType,
        String logicalOperator,
        int executionOrder,
        List<RuleCondition> conditions
) {
    public RuleConditionGroup {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }
}
