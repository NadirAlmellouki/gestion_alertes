package FST.MST_RSI.PFA.classification.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Compact alert slice sent to the LLM (not the full Dynatrace JSON).
 */
public record AlertClassificationContext(
        String alertId,
        String externalProblemId,
        String title,
        String severityLevel,
        String impactLevel,
        String status,
        String applicationName,
        String environment,
        String hostName,
        String rootCauseEntity,
        List<String> affectedEntityNames,
        List<String> impactedEntityNames,
        List<Map<String, String>> entityTags,
        List<String> managementZones,
        List<String> k8sNamespaces,
        String evidenceSummary
) {
}
