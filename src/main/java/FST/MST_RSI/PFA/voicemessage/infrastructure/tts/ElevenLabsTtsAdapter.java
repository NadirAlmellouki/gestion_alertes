package FST.MST_RSI.PFA.voicemessage.infrastructure.tts;

import FST.MST_RSI.PFA.voicemessage.domain.model.TtsAudio;
import FST.MST_RSI.PFA.voicemessage.domain.port.TextToSpeechPort;
import FST.MST_RSI.PFA.voicemessage.infrastructure.config.ElevenLabsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "app.voice.tts", name = "provider", havingValue = "elevenlabs")
public class ElevenLabsTtsAdapter implements TextToSpeechPort {

    private static final Logger log = LoggerFactory.getLogger(ElevenLabsTtsAdapter.class);

    private final ElevenLabsProperties properties;

    public ElevenLabsTtsAdapter(ElevenLabsProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<TtsAudio> synthesize(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.warn("ElevenLabs API key not configured; skipping TTS");
            return Optional.empty();
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = (int) Math.min(Duration.ofSeconds(properties.getTimeoutSeconds()).toMillis(), Integer.MAX_VALUE);
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        RestClient client = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("xi-api-key", properties.getApiKey())
                .defaultHeader("Accept", "audio/mpeg")
                .build();

        try {
            byte[] audio = client.post()
                    .uri("/v1/text-to-speech/{voiceId}", properties.getVoiceId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "text", text,
                            "model_id", properties.getModelId()
                    ))
                    .retrieve()
                    .body(byte[].class);
            if (audio == null || audio.length == 0) {
                return Optional.empty();
            }
            return Optional.of(new TtsAudio(audio, "audio/mpeg"));
        } catch (Exception ex) {
            log.warn("ElevenLabs TTS failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
