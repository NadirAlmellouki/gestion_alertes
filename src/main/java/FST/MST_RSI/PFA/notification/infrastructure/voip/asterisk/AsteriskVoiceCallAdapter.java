package FST.MST_RSI.PFA.notification.infrastructure.voip.asterisk;

import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryResult;
import FST.MST_RSI.PFA.notification.domain.model.VoiceCallRequest;
import FST.MST_RSI.PFA.notification.domain.port.VoiceCallPort;
import FST.MST_RSI.PFA.notification.domain.service.SipEndpointMapper;
import FST.MST_RSI.PFA.notification.infrastructure.config.VoipNotificationProperties;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionEntity;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnExpression("${app.notification.voip.enabled:false} && '${app.notification.voip.provider:local}'.equals('asterisk')")
public class AsteriskVoiceCallAdapter implements VoiceCallPort {

    private static final Logger log = LoggerFactory.getLogger(AsteriskVoiceCallAdapter.class);

    private final VoipNotificationProperties properties;
    private final AsteriskAriClient ariClient;
    private final VoiceCallSessionJpaRepository sessionRepository;

    public AsteriskVoiceCallAdapter(
            VoipNotificationProperties properties,
            AsteriskAriClient ariClient,
            VoiceCallSessionJpaRepository sessionRepository
    ) {
        this.properties = properties;
        this.ariClient = ariClient;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public NotificationDeliveryResult call(VoiceCallRequest request) {
        try {
            String extension = SipEndpointMapper.extensionFromPhone(request.phoneNumber());
            String soundName = persistAudio(request);
            String channelId = AsteriskAriClient.newChannelId();
            UUID notificationId = parseUuid(request.correlationId());

            VoiceCallSessionEntity session = new VoiceCallSessionEntity();
            session.setId(UUID.randomUUID());
            session.setNotificationId(notificationId);
            session.setAlertId(request.alertId());
            session.setRoutingExecutionId(request.routingExecutionId());
            session.setPersonId(request.personId());
            session.setExtension(extension);
            session.setProviderCallId(channelId);
            session.setSoundName(soundName);
            session.setOutcome("INITIATED");
            session.setStartedAt(Instant.now());
            session.setLiveConversation(request.liveConversation());
            sessionRepository.save(session);

            String args = "sound=" + (soundName == null ? "" : soundName)
                    + ",notification=" + (notificationId == null ? "" : notificationId)
                    + ",live=" + request.liveConversation()
                    + ",session=" + session.getId();
            log.info("[VOICE] Requesting call destination={} live={}", extension, request.liveConversation());
            ariClient.originate(
                    SipEndpointMapper.pjsipEndpoint(extension),
                    args,
                    properties.getTimeoutSeconds(),
                    channelId
            );
            return NotificationDeliveryResult.sent(channelId);
        } catch (Exception ex) {
            log.warn("[VOICE] Originate failed: {}", ex.getMessage());
            return NotificationDeliveryResult.failed(ex.getMessage());
        }
    }

    private String persistAudio(VoiceCallRequest request) throws IOException, InterruptedException {
        if (request.liveConversation()) {
            return null;
        }
        byte[] content = request.audioContent();
        String contentType = request.audioContentType();
        if (content == null || content.length == 0) {
            var tone = FST.MST_RSI.PFA.voicemessage.infrastructure.tts.PcmWavTone.shortAlertTone();
            content = tone.content();
            contentType = tone.contentType();
        }
        Path dir = Path.of(properties.getAudioDir()).toAbsolutePath();
        Files.createDirectories(dir);
        String base = "call-" + UUID.randomUUID();
        Path source = dir.resolve(base + sourceExtension(contentType));
        Files.write(source, content);
        Path wav = dir.resolve(base + ".wav");
        convertToWav(source, wav);
        return base;
    }

    private static String sourceExtension(String contentType) {
        if (contentType != null && contentType.contains("wav")) {
            return ".wav";
        }
        return ".mp3";
    }

    private static void convertToWav(Path source, Path wav) throws IOException, InterruptedException {
        if (source.getFileName().toString().endsWith(".wav")) {
            if (!source.equals(wav)) {
                Files.copy(source, wav);
            }
            return;
        }
        Process process = new ProcessBuilder(
                "ffmpeg", "-y", "-i", source.toString(),
                "-ac", "1", "-ar", "8000", "-acodec", "pcm_s16le",
                wav.toString()
        ).redirectErrorStream(true).start();
        int code = process.waitFor();
        if (code != 0 || !Files.exists(wav)) {
            Files.copy(source, wav);
            log.warn("[VOICE] ffmpeg conversion failed code={}; using original bytes as {}", code, wav);
        }
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
