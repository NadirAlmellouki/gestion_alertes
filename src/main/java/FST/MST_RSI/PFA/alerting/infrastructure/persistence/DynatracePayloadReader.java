package FST.MST_RSI.PFA.alerting.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DynatracePayloadReader {

    private final ObjectMapper objectMapper;

    public DynatracePayloadReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode readTree(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(rawPayload);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    public String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    public String extractApplicationName(JsonNode root) {
        String fromService = firstEntityName(root.path("affectedEntities"), "SERVICE");
        if (fromService != null) {
            return fromService;
        }
        return firstEntityName(root.path("impactedEntities"), "APPLICATION");
    }

    public String extractEnvironment(JsonNode root) {
        if (root.path("entityTags").isArray()) {
            for (JsonNode tag : root.path("entityTags")) {
                if ("environment".equalsIgnoreCase(text(tag, "key"))) {
                    return text(tag, "value");
                }
            }
        }
        if (root.path("managementZones").isArray() && !root.path("managementZones").isEmpty()) {
            return text(root.path("managementZones").get(0), "name");
        }
        return null;
    }

    public String extractHostName(JsonNode root) {
        String fromRoot = entityName(root.path("rootCauseEntity"));
        if (fromRoot != null) {
            return fromRoot;
        }
        return firstEntityName(root.path("affectedEntities"), "HOST");
    }

    public String extractProblemUrl(JsonNode root) {
        return text(root, "problemUrl");
    }

    private String firstEntityName(JsonNode array, String type) {
        if (!array.isArray()) {
            return null;
        }
        for (JsonNode node : array) {
            String entityType = text(node.path("entityId"), "type");
            if (type.equalsIgnoreCase(entityType)) {
                return entityName(node);
            }
        }
        return entityName(array.isEmpty() ? null : array.get(0));
    }

    private String entityName(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return text(node, "name");
    }
}
