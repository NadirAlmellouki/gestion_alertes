package FST.MST_RSI.PFA.rulesengine.domain.model;

import java.util.UUID;

public record RuleAction(
        UUID id,
        String actionType,
        String actionValue,
        int executionOrder
) {
}
