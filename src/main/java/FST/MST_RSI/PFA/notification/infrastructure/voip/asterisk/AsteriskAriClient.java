package FST.MST_RSI.PFA.notification.infrastructure.voip.asterisk;

import FST.MST_RSI.PFA.notification.infrastructure.config.VoipNotificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Component
public class AsteriskAriClient {

    private static final Logger log = LoggerFactory.getLogger(AsteriskAriClient.class);

    private final VoipNotificationProperties properties;

    public AsteriskAriClient(VoipNotificationProperties properties) {
        this.properties = properties;
    }

    public String originate(String endpoint, String appArgs, int timeoutSeconds, String channelId) {
        Map<?, ?> response = client().post()
                .uri(uriBuilder -> uriBuilder
                        .path("/ari/channels")
                        .queryParam("endpoint", endpoint)
                        .queryParam("app", "alertops")
                        .queryParam("appArgs", appArgs)
                        .queryParam("timeout", timeoutSeconds)
                        .queryParam("channelId", channelId)
                        .queryParam("callerId", "AlertOps")
                        .build())
                .retrieve()
                .body(Map.class);
        String id = response != null && response.get("id") != null ? response.get("id").toString() : channelId;
        log.info("[VOICE] ARI originate endpoint={} callId={}", endpoint, id);
        return id;
    }

    public void answer(String channelId) {
        client().post().uri("/ari/channels/{id}/answer", channelId).retrieve().toBodilessEntity();
    }

    public void play(String channelId, String soundName) {
        client().post()
                .uri("/ari/channels/{id}/play?media=sound:alertops/{sound}", channelId, soundName)
                .retrieve()
                .toBodilessEntity();
        log.info("[VOICE] Playing audio={} on channel={}", soundName, channelId);
    }

    public void record(String channelId, String recordingName) {
        try {
            client().post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ari/channels/{id}/record")
                            .queryParam("name", recordingName)
                            .queryParam("format", "wav")
                            .queryParam("ifExists", "overwrite")
                            .queryParam("beep", "false")
                            .build(channelId))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[VOICE] Recording started on channel={} recordingName={}", channelId, recordingName);
        } catch (Exception ex) {
            log.warn("[VOICE] Failed to start recording on channel {}: {}", channelId, ex.getMessage());
        }
    }

    /**
     * Creates a mixing bridge between two channels (supervisor ↔ admin).
     * Returns the bridgeId.
     */
    public String bridge(String channelId1, String channelId2) {
        try {
            // Create a mixing bridge
            Map<?, ?> bridge = client().post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ari/bridges")
                            .queryParam("type", "mixing")
                            .build())
                    .retrieve()
                    .body(Map.class);
            String bridgeId = bridge != null && bridge.get("id") != null ? bridge.get("id").toString() : null;
            if (bridgeId == null) {
                log.warn("[VOICE] Bridge creation returned null id");
                return null;
            }
            // Add both channels to the bridge individually
            client().post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ari/bridges/{id}/addChannel")
                            .queryParam("channel", channelId1)
                            .build(bridgeId))
                    .retrieve()
                    .toBodilessEntity();
            client().post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ari/bridges/{id}/addChannel")
                            .queryParam("channel", channelId2)
                            .build(bridgeId))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[VOICE] Bridge created bridgeId={} between channel1={} channel2={}", bridgeId, channelId1, channelId2);
            return bridgeId;
        } catch (Exception ex) {
            log.warn("[VOICE] Failed to create bridge between {} and {}: {}", channelId1, channelId2, ex.getMessage());
            return null;
        }
    }

    /**
     * Starts a recording on the bridge (records both channels mixed).
     */
    public void recordBridge(String bridgeId, String recordingName) {
        try {
            client().post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ari/bridges/{id}/record")
                            .queryParam("name", recordingName)
                            .queryParam("format", "wav")
                            .queryParam("ifExists", "overwrite")
                            .queryParam("beep", "false")
                            .build(bridgeId))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[VOICE] Bridge recording started bridgeId={} name={}", bridgeId, recordingName);
        } catch (Exception ex) {
            log.warn("[VOICE] Failed to start bridge recording {}: {}", bridgeId, ex.getMessage());
        }
    }

    public void stopRecording(String recordingName) {
        try {
            client().post()
                    .uri("/ari/recordings/live/{name}/stop", recordingName)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[VOICE] Bridge recording stopped name={}", recordingName);
        } catch (Exception ex) {
            log.debug("[VOICE] Recording stop ignored for {}: {}", recordingName, ex.getMessage());
        }
    }

    public void destroyBridge(String bridgeId) {
        try {
            client().delete().uri("/ari/bridges/{id}", bridgeId).retrieve().toBodilessEntity();
            log.info("[VOICE] Bridge destroyed bridgeId={}", bridgeId);
        } catch (Exception ex) {
            log.debug("[VOICE] Bridge destroy ignored for {}: {}", bridgeId, ex.getMessage());
        }
    }

    public void hangup(String channelId) {
        try {
            client().delete().uri("/ari/channels/{id}", channelId).retrieve().toBodilessEntity();
        } catch (Exception ex) {
            log.debug("[VOICE] Hangup ignored for {}: {}", channelId, ex.getMessage());
        }
    }

    public String eventsWebSocketUrl() {
        String http = properties.getAriUrl() == null ? "http://localhost:8088" : properties.getAriUrl();
        String ws = http.replaceFirst("^http", "ws");
        return ws + "/ari/events?app=alertops&subscribeAll=true&api_key="
                + properties.getAriUser() + ":" + properties.getAriPassword();
    }

    private RestClient client() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        String basic = Base64.getEncoder().encodeToString(
                (properties.getAriUser() + ":" + properties.getAriPassword()).getBytes(StandardCharsets.UTF_8)
        );
        return RestClient.builder()
                .baseUrl(properties.getAriUrl())
                .requestFactory(factory)
                .defaultHeader("Authorization", "Basic " + basic)
                .build();
    }

    public static String newChannelId() {
        return UUID.randomUUID().toString();
    }
}
