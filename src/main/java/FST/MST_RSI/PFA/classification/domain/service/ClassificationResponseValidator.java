package FST.MST_RSI.PFA.classification.domain.service;

import FST.MST_RSI.PFA.classification.domain.model.ClassificationCategory;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationStatus;
import FST.MST_RSI.PFA.classification.domain.model.SolutionContext;
import FST.MST_RSI.PFA.common.domain.vo.Confidence;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class ClassificationResponseValidator {

    private static final double LOW_CONFIDENCE_THRESHOLD = 0.55;

    private final ObjectMapper objectMapper;

    public ClassificationResponseValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ClassificationResult parseAndValidate(String rawJson, List<SolutionContext> candidates) {
        try {
            String cleaned = extractJsonObject(rawJson);
            JsonNode root = objectMapper.readTree(cleaned);

            if (root.has("proposedPriority") || root.has("priority") || root.has("psi")) {
                return ClassificationResult.fallback("LLM must not return priority/PSI fields");
            }

            ClassificationCategory category = parseCategory(text(root, "category"));
            double confidenceValue = root.path("confidence").asDouble(-1);
            if (confidenceValue < 0 || confidenceValue > 1) {
                return ClassificationResult.fallback("Invalid confidence value");
            }

            List<String> uncertain = new ArrayList<>();
            if (root.path("uncertainFields").isArray()) {
                root.path("uncertainFields").forEach(n -> uncertain.add(n.asText()));
            }

            boolean requiresValidation = root.path("requiresHumanValidation").asBoolean(false)
                    || confidenceValue < LOW_CONFIDENCE_THRESHOLD;

            ClassificationStatus status = confidenceValue < LOW_CONFIDENCE_THRESHOLD
                    ? ClassificationStatus.LOW_CONFIDENCE
                    : ClassificationStatus.SUCCESS;

            String problemType = text(root, "problemType");
            if (problemType == null || problemType.isBlank()) {
                return ClassificationResult.fallback("Missing problemType");
            }

            String summary = text(root, "summary");
            if (summary == null || summary.isBlank()) {
                return ClassificationResult.fallback("Missing summary");
            }

            String matchedSolution = text(root, "matchedSolution");
            if (matchedSolution != null && !isKnownSolution(matchedSolution, candidates)) {
                return ClassificationResult.fallback("matchedSolution not in retrieved candidates");
            }

            if (root.path("fallback").asBoolean(false)) {
                status = ClassificationStatus.FALLBACK;
                requiresValidation = true;
            }

            return new ClassificationResult(
                    category,
                    problemType,
                    new Confidence(confidenceValue),
                    matchedSolution,
                    text(root, "matchedDomaine"),
                    text(root, "matchedPole"),
                    text(root, "matchedEntity"),
                    summary,
                    text(root, "probableCause"),
                    text(root, "justification"),
                    uncertain,
                    requiresValidation,
                    status,
                    null,
                    null
            );
        } catch (Exception ex) {
            return ClassificationResult.fallback("Invalid LLM JSON: " + ex.getMessage());
        }
    }

    private boolean isKnownSolution(String matchedSolution, List<SolutionContext> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return false;
        }
        return candidates.stream()
                .anyMatch(candidate -> candidate.name() != null
                        && candidate.name().equalsIgnoreCase(matchedSolution.trim()));
    }

    private String extractJsonObject(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        return trimmed;
    }

    private ClassificationCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return ClassificationCategory.UNKNOWN;
        }
        try {
            return ClassificationCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ClassificationCategory.UNKNOWN;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }
}
