package FST.MST_RSI.PFA.rulesengine.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record BusinessDecision(
        UUID matchedRuleId,
        String matchedRuleCode,
        RuleOrigin matchedRuleOrigin,
        boolean humanValidationRequired,
        boolean routingTriggered,
        UUID selectedSolutionUnitId,
        String selectedSolutionName,
        String forcedRole,
        List<String> tags,
        String enrichment,
        List<String> appliedActions
) {
    public BusinessDecision {
        tags = tags == null ? List.of() : List.copyOf(tags);
        appliedActions = appliedActions == null ? List.of() : List.copyOf(appliedActions);
    }

    public static BusinessDecision noMatch() {
        return new BusinessDecision(null, null, null, false, false, null, null, null, List.of(), null, List.of());
    }

    public BusinessDecision merge(BusinessDecision other) {
        List<String> actions = new ArrayList<>(appliedActions);
        actions.addAll(other.appliedActions());
        return new BusinessDecision(
                other.matchedRuleId() != null ? other.matchedRuleId() : matchedRuleId,
                other.matchedRuleCode() != null ? other.matchedRuleCode() : matchedRuleCode,
                other.matchedRuleOrigin() != null ? other.matchedRuleOrigin() : matchedRuleOrigin,
                humanValidationRequired || other.humanValidationRequired,
                routingTriggered || other.routingTriggered,
                other.selectedSolutionUnitId() != null ? other.selectedSolutionUnitId() : selectedSolutionUnitId,
                other.selectedSolutionName() != null ? other.selectedSolutionName() : selectedSolutionName,
                other.forcedRole() != null ? other.forcedRole() : forcedRole,
                other.tags().isEmpty() ? tags : other.tags(),
                other.enrichment() != null ? other.enrichment() : enrichment,
                actions
        );
    }
}
