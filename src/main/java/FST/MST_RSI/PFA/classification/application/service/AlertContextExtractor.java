package FST.MST_RSI.PFA.classification.application.service;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.classification.domain.model.AlertClassificationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds a compact LLM context from an Alert + raw Dynatrace JSON (Problems V2).
 */
@Component
public class AlertContextExtractor {

    private final ObjectMapper objectMapper;

    public AlertContextExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AlertClassificationContext extract(Alert alert) {
        JsonNode root = readRaw(alert.getRawPayload());

        return new AlertClassificationContext(
                alert.getId().value().toString(),
                alert.getExternalProblemId(),
                alert.getTitle(),
                firstNonBlank(alert.getSeverity(), text(root, "severityLevel")),
                firstNonBlank(alert.getImpact(), text(root, "impactLevel")),
                firstNonBlank(alert.getDynatraceState(), text(root, "status")),
                alert.getApplicationName(),
                alert.getEnvironment(),
                alert.getHostName(),
                entityName(root.path("rootCauseEntity")),
                entityNames(root.path("affectedEntities")),
                entityNames(root.path("impactedEntities")),
                tags(root.path("entityTags")),
                names(root.path("managementZones")),
                stringArray(root.path("k8s.namespace.name")),
                evidenceSummary(root.path("evidenceDetails"))
        );
    }

    public String[] searchTerms(Alert alert, AlertClassificationContext context) {
        List<String> terms = new ArrayList<>();
        add(terms, alert.getApplicationName());
        add(terms, alert.getHostName());
        add(terms, context.rootCauseEntity());
        context.affectedEntityNames().forEach(n -> add(terms, n));
        context.impactedEntityNames().forEach(n -> add(terms, n));
        List<String> stopWords = List.of("service", "services", "production", "prod", "indisponibilite", "erreur", "error", "serveur", "server", "authentification", "unavailable", "failure", "alert", "problem", "incident");
        if (alert.getTitle() != null) {
            for (String word : alert.getTitle().split("[\\s,;:\\-_/()'\"]+")) {
                if (word.length() >= 3 && !stopWords.contains(word.toLowerCase(Locale.ROOT))) {
                    add(terms, word);
                }
            }
        }
        if (context.entityTags() != null) {
            for (Map<String, String> tag : context.entityTags()) {
                add(terms, tag.get("value"));
            }
        }
        return terms.toArray(String[]::new);
    }

    private JsonNode readRaw(String rawPayload) {
        try {
            return objectMapper.readTree(rawPayload);
        } catch (Exception ex) {
            return objectMapper.createObjectNode();
        }
    }

    private List<String> entityNames(JsonNode array) {
        List<String> names = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(n -> {
                String name = entityName(n);
                if (name != null) {
                    names.add(name);
                }
            });
        }
        return names;
    }

    private String entityName(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return text(node, "name");
    }

    private List<Map<String, String>> tags(JsonNode array) {
        List<Map<String, String>> tags = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(n -> {
                Map<String, String> tag = new HashMap<>();
                String key = text(n, "key");
                String value = text(n, "value");
                if (key != null) {
                    tag.put("key", key);
                    tag.put("value", value == null ? "" : value);
                    tags.add(tag);
                }
            });
        }
        return tags;
    }

    private List<String> names(JsonNode array) {
        List<String> names = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(n -> {
                String name = text(n, "name");
                if (name != null) {
                    names.add(name);
                }
            });
        }
        return names;
    }

    private List<String> stringArray(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(n -> {
                if (!n.isNull() && !n.asText().isBlank()) {
                    values.add(n.asText());
                }
            });
        }
        return values;
    }

    private String evidenceSummary(JsonNode evidenceDetails) {
        if (evidenceDetails == null || evidenceDetails.isMissingNode()) {
            return null;
        }
        JsonNode details = evidenceDetails.path("details");
        if (!details.isArray() || details.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (JsonNode detail : details) {
            if (detail.path("rootCauseRelevant").asBoolean(false)) {
                String display = text(detail, "displayName");
                String entity = entityName(detail.path("entity"));
                parts.add((display == null ? "evidence" : display) + (entity == null ? "" : " @ " + entity));
            }
            if (parts.size() >= 3) {
                break;
            }
        }
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private String text(JsonNode node, String field) {
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

    private void add(List<String> terms, String value) {
        if (value != null && !value.isBlank() && !terms.contains(value)) {
            terms.add(value);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
