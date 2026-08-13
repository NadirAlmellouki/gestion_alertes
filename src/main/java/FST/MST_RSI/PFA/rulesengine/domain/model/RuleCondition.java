package FST.MST_RSI.PFA.rulesengine.domain.model;

import java.util.List;
import java.util.UUID;

public record RuleCondition(
        UUID id,
        String fieldName,
        ConditionOperator operator,
        String expectedValue,
        String valueType,
        int conditionOrder
) {
}
