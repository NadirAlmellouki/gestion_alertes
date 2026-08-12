package FST.MST_RSI.PFA.common.infrastructure.llm;

import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Map;

/**
 * Low-level LLM HTTP client shared by classification and future voicemessage.
 * No business semantics — only transport concerns.
 */
@Component
public class LlmHttpClient {

    public String postJson(String baseUrl, String path, Map<String, String> queryParams, Object body, Duration timeout) {
        ClientHttpRequestFactory requestFactory = requestFactory(timeout);
        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        try {
            return client.post()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path);
                        if (queryParams != null) {
                            queryParams.forEach(uriBuilder::queryParam);
                        }
                        return uriBuilder.build();
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            throw new LlmClientException("LLM provider returned HTTP " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            throw new LlmClientException("LLM provider unreachable", ex);
        } catch (Exception ex) {
            throw new LlmClientException("LLM call failed: " + ex.getMessage(), ex);
        }
    }

    private static ClientHttpRequestFactory requestFactory(Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int millis = (int) Math.min(timeout.toMillis(), Integer.MAX_VALUE);
        factory.setConnectTimeout(millis);
        factory.setReadTimeout(millis);
        return factory;
    }
}
