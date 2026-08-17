package FST.MST_RSI.PFA.monitoring.infrastructure.dynatrace;

import FST.MST_RSI.PFA.monitoring.domain.model.DynatraceProblemSnapshot;
import FST.MST_RSI.PFA.monitoring.domain.port.DynatraceProblemPort;
import FST.MST_RSI.PFA.monitoring.infrastructure.config.DynatraceApiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "app.monitoring.dynatrace", name = "enabled", havingValue = "true")
public class DynatraceApiAdapter implements DynatraceProblemPort {

    private static final Logger log = LoggerFactory.getLogger(DynatraceApiAdapter.class);

    private final DynatraceApiProperties properties;
    private final ObjectMapper objectMapper;

    public DynatraceApiAdapter(DynatraceApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<DynatraceProblemSnapshot> fetchProblem(String externalProblemId) {
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()
                || properties.getApiToken() == null || properties.getApiToken().isBlank()) {
            log.warn("Dynatrace API not configured; cannot fetch problem {}", externalProblemId);
            return Optional.empty();
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = (int) Math.min(Duration.ofSeconds(properties.getTimeoutSeconds()).toMillis(), Integer.MAX_VALUE);
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        RestClient client = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Api-Token " + properties.getApiToken())
                .build();

        try {
            String body = client.get()
                    .uri("/api/v2/problems/{problemId}", externalProblemId)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(body);
            String status = text(root, "status");
            String problemId = text(root, "problemId");
            if (status == null) {
                return Optional.empty();
            }
            return Optional.of(new DynatraceProblemSnapshot(
                    problemId != null ? problemId : externalProblemId,
                    status
            ));
        } catch (Exception ex) {
            log.warn("Dynatrace API call failed for problem {}: {}", externalProblemId, ex.getMessage());
            return Optional.empty();
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
