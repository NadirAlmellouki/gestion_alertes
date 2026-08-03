package FST.MST_RSI.PFA.alerting.domain.service;

import FST.MST_RSI.PFA.alerting.domain.model.RawAlertPayload;
import FST.MST_RSI.PFA.alerting.domain.model.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultAlertPayloadValidator implements AlertPayloadValidator {

    private static final int MAX_PAYLOAD_SIZE = 256_000;

    private final ObjectMapper objectMapper;

    public DefaultAlertPayloadValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ValidationResult validate(RawAlertPayload payload) {
        List<String> errors = new ArrayList<>();

        if (payload.jsonBody().length() > MAX_PAYLOAD_SIZE) {
            errors.add("Payload exceeds maximum size of " + MAX_PAYLOAD_SIZE + " bytes");
            return ValidationResult.failure(errors);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload.jsonBody());
        } catch (Exception ex) {
            return ValidationResult.failure("Payload is not valid JSON");
        }

        if (!root.isObject()) {
            return ValidationResult.failure("Payload root must be a JSON object");
        }

        if (!hasProblemIdentifier(root)) {
            errors.add("Missing problem identifier (PID, ProblemID or problemId)");
        }

        if (!hasTitle(root)) {
            errors.add("Missing problem title (ProblemTitle or title)");
        }

        if (errors.isEmpty()) {
            return ValidationResult.success();
        }
        return ValidationResult.failure(errors);
    }

    private boolean hasProblemIdentifier(JsonNode root) {
        JsonNode problemDetails = root.path("ProblemDetailsJSONv2");
        if (!problemDetails.isMissingNode() && !problemDetails.isNull()) {
            if (problemDetails.isTextual()) {
                try {
                    problemDetails = objectMapper.readTree(problemDetails.asText());
                } catch (Exception ex) {
                    return hasText(root, "PID") || hasText(root, "ProblemID");
                }
            }
            if (problemDetails.hasNonNull("problemId")) {
                return true;
            }
        }
        return hasText(root, "PID")
                || hasText(root, "ProblemID")
                || hasText(root, "problemId");
    }

    private boolean hasTitle(JsonNode root) {
        JsonNode problemDetails = root.path("ProblemDetailsJSONv2");
        if (!problemDetails.isMissingNode() && !problemDetails.isNull()) {
            if (problemDetails.isTextual()) {
                try {
                    problemDetails = objectMapper.readTree(problemDetails.asText());
                } catch (Exception ex) {
                    return hasText(root, "ProblemTitle");
                }
            }
            if (problemDetails.hasNonNull("title") && !problemDetails.get("title").asText().isBlank()) {
                return true;
            }
        }
        return hasText(root, "ProblemTitle") || hasText(root, "title");
    }

    private boolean hasText(JsonNode node, String field) {
        return node.hasNonNull(field) && !node.get(field).asText().isBlank();
    }
}
