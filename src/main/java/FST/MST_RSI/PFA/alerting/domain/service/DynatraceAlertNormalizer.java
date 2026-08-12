package FST.MST_RSI.PFA.alerting.domain.service;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Iterator;

@Component
public class DynatraceAlertNormalizer {

    private final ObjectMapper objectMapper;

    public DynatraceAlertNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NormalizedAlertData normalize(String jsonBody) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode problem = extractProblemNode(root);

            String externalProblemId = firstNonBlank(
                    text(problem, "problemId"),
                    text(root, "PID"),
                    text(root, "ProblemID"),
                    text(root, "problemId")
            );

            String title = firstNonBlank(
                    text(problem, "title"),
                    text(root, "ProblemTitle"),
                    text(root, "title")
            );

            String severity = firstNonBlank(
                    text(problem, "severityLevel"),
                    text(root, "ProblemSeverity"),
                    text(root, "severityLevel")
            );

            String impact = firstNonBlank(
                    text(problem, "impactLevel"),
                    text(root, "ProblemImpact"),
                    text(root, "impactLevel")
            );

            String state = firstNonBlank(
                    text(problem, "status"),
                    text(root, "State"),
                    text(root, "status")
            );

            String problemUrl = firstNonBlankOrNull(
                    text(root, "ProblemURL"),
                    text(root, "problemUrl")
            );

            String applicationName = extractApplicationName(problem, root);
            String environment = extractEnvironment(problem, root);
            String hostName = extractHostName(problem, root);
            Instant problemStartedAt = extractStartTime(problem);

            return new NormalizedAlertData(
                    require(externalProblemId, "problemId"),
                    require(title, "title"),
                    nullIfUnknown(applicationName),
                    environment,
                    nullIfUnknown(severity),
                    nullIfUnknown(impact),
                    nullIfUnknown(state),
                    problemUrl,
                    hostName,
                    jsonBody,
                    problemStartedAt
            );
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to normalize Dynatrace payload", ex);
        }
    }

    private JsonNode extractProblemNode(JsonNode root) {
        JsonNode problemDetails = root.path("ProblemDetailsJSONv2");
        if (problemDetails.isMissingNode() || problemDetails.isNull()) {
            return root;
        }
        if (problemDetails.isTextual()) {
            try {
                return objectMapper.readTree(problemDetails.asText());
            } catch (Exception ex) {
                return root;
            }
        }
        if (problemDetails.isObject()) {
            return problemDetails;
        }
        return root;
    }

    private String extractApplicationName(JsonNode problem, JsonNode root) {
        String fromEntity = firstEntityName(problem.path("affectedEntities"));
        if (fromEntity != null) {
            return fromEntity;
        }
        return firstNonBlank(
                text(root, "ImpactedEntity"),
                text(root, "NamesOfImpactedEntities")
        );
    }

    private String extractEnvironment(JsonNode problem, JsonNode root) {
        JsonNode tags = problem.path("entityTags");
        if (tags.isArray()) {
            for (JsonNode tag : tags) {
                String key = text(tag, "key");
                if ("environment".equalsIgnoreCase(key) || "env".equalsIgnoreCase(key)) {
                    return text(tag, "value");
                }
            }
        }
        String impacted = text(root, "ImpactedEntity");
        if (impacted != null) {
            if (impacted.toLowerCase().contains("production")) {
                return "Production";
            }
            if (impacted.toLowerCase().contains("recette")) {
                return "Recette";
            }
        }
        return null;
    }

    private String extractHostName(JsonNode problem, JsonNode root) {
        String fromEntity = firstEntityName(problem.path("affectedEntities"));
        if (fromEntity != null) {
            return fromEntity;
        }
        return text(root, "ImpactedEntityNames");
    }

    private Instant extractStartTime(JsonNode problem) {
        JsonNode startTime = problem.get("startTime");
        if (startTime == null || startTime.isNull()) {
            return null;
        }
        long epochMillis = startTime.asLong(-1);
        if (epochMillis <= 0) {
            return null;
        }
        return Instant.ofEpochMilli(epochMillis);
    }

    private String firstEntityName(JsonNode entities) {
        if (!entities.isArray()) {
            return null;
        }
        Iterator<JsonNode> iterator = entities.elements();
        while (iterator.hasNext()) {
            JsonNode entity = iterator.next();
            String name = text(entity, "name");
            if (name != null) {
                return name;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "UNKNOWN";
    }

    private String firstNonBlankOrNull(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String require(String value, String field) {
        if (value == null || value.isBlank() || "UNKNOWN".equals(value)) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return value;
    }

    private String nullIfUnknown(String value) {
        return "UNKNOWN".equals(value) ? null : value;
    }

    public record NormalizedAlertData(
            String externalProblemId,
            String title,
            String applicationName,
            String environment,
            String severity,
            String impact,
            String dynatraceState,
            String problemUrl,
            String hostName,
            String rawPayload,
            Instant problemStartedAt
    ) {
    }
}
