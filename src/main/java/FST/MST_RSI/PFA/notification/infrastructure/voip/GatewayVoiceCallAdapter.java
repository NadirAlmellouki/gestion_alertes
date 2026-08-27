package FST.MST_RSI.PFA.notification.infrastructure.voip;

import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryResult;
import FST.MST_RSI.PFA.notification.domain.model.VoiceCallRequest;
import FST.MST_RSI.PFA.notification.domain.port.VoiceCallPort;
import FST.MST_RSI.PFA.notification.infrastructure.config.VoipNotificationProperties;
import FST.MST_RSI.PFA.voicemessage.domain.port.TextToSpeechPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnExpression("${app.notification.voip.enabled:false} && '${app.notification.voip.provider:local}'.equals('gateway')")
public class GatewayVoiceCallAdapter implements VoiceCallPort {

    private static final Logger log = LoggerFactory.getLogger(GatewayVoiceCallAdapter.class);

    private final VoipNotificationProperties properties;
    private final TextToSpeechPort textToSpeechPort;

    public GatewayVoiceCallAdapter(VoipNotificationProperties properties, TextToSpeechPort textToSpeechPort) {
        this.properties = properties;
        this.textToSpeechPort = textToSpeechPort;
    }

    @Override
    public NotificationDeliveryResult call(VoiceCallRequest request) {
        if (properties.getGatewayUrl() == null || properties.getGatewayUrl().isBlank()) {
            return NotificationDeliveryResult.failed("VoIP gateway URL is not configured");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("alertId", request.alertId() != null ? request.alertId().toString() : null);
        payload.put("routingExecutionId", request.routingExecutionId() != null ? request.routingExecutionId().toString() : null);
        payload.put("personId", request.personId() != null ? request.personId().toString() : null);
        payload.put("phoneNumber", request.phoneNumber());
        payload.put("recipientName", request.recipientName());
        payload.put("message", request.message());
        payload.put("correlationId", request.correlationId());

        payload.put("liveConversation", request.liveConversation());
        payload.put("callMode", request.liveConversation() ? "MANUAL" : "AUTO");

        if (!request.liveConversation()) {
            textToSpeechPort.synthesize(request.message()).ifPresent(audio -> {
                payload.put("audioContentType", audio.contentType());
                payload.put("audioBase64", java.util.Base64.getEncoder().encodeToString(audio.content()));
            });
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(30_000);

        RestClient client = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        try {
            client.post()
                    .uri(properties.getGatewayUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            String callId = UUID.randomUUID().toString();
            log.info("VOIP gateway call accepted callId={} extension={}", callId, request.phoneNumber());
            return NotificationDeliveryResult.sent(callId);
        } catch (Exception ex) {
            log.warn("VOIP gateway call failed for extension {}: {}", request.phoneNumber(), ex.getMessage());
            return NotificationDeliveryResult.failed(ex.getMessage());
        }
    }
}
