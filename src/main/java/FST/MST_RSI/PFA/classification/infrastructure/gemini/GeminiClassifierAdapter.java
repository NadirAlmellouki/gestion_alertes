package FST.MST_RSI.PFA.classification.infrastructure.gemini;

import FST.MST_RSI.PFA.classification.domain.model.AlertClassificationContext;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.classification.domain.model.SolutionContext;
import FST.MST_RSI.PFA.classification.domain.port.AlertClassifierPort;
import FST.MST_RSI.PFA.classification.domain.service.ClassificationPromptBuilder;
import FST.MST_RSI.PFA.classification.domain.service.ClassificationResponseValidator;
import FST.MST_RSI.PFA.common.infrastructure.llm.LlmClientException;
import FST.MST_RSI.PFA.common.infrastructure.llm.LlmHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.llm", name = "provider", havingValue = "gemini", matchIfMissing = true)
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiClassifierAdapter implements AlertClassifierPort {

    private final LlmHttpClient llmHttpClient;
    private final GeminiProperties properties;
    private final ClassificationPromptBuilder promptBuilder;
    private final ClassificationResponseValidator responseValidator;
    private final ObjectMapper objectMapper;

    public GeminiClassifierAdapter(
            LlmHttpClient llmHttpClient,
            GeminiProperties properties,
            ClassificationPromptBuilder promptBuilder,
            ClassificationResponseValidator responseValidator,
            ObjectMapper objectMapper
    ) {
        this.llmHttpClient = llmHttpClient;
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.responseValidator = responseValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public ClassificationResult classify(AlertClassificationContext alertContext, List<SolutionContext> businessContext) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            return ClassificationResult.fallback("GEMINI_API_KEY is not configured");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(
                                Map.of("text", promptBuilder.systemInstructions() + "\n\n" + promptBuilder.userPrompt(alertContext, businessContext))
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "responseMimeType", "application/json"
                )
        );

        String path = "/v1beta/models/" + properties.model() + ":generateContent";
        try {
            String response = llmHttpClient.postJson(
                    properties.baseUrl(),
                    path,
                    Map.of("key", properties.apiKey()),
                    body,
                    Duration.ofSeconds(Math.max(1, properties.timeoutSeconds()))
            );
            String text = extractText(response);
            return responseValidator.parseAndValidate(text, businessContext);
        } catch (LlmClientException ex) {
            return ClassificationResult.fallback(ex.getMessage());
        }
    }

    private String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new LlmClientException("Gemini response missing text content");
            }
            return textNode.asText();
        } catch (LlmClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LlmClientException("Unable to parse Gemini response", ex);
        }
    }
}
