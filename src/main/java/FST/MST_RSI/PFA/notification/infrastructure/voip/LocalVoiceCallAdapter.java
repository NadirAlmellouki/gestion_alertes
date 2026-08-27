package FST.MST_RSI.PFA.notification.infrastructure.voip;

import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryResult;
import FST.MST_RSI.PFA.notification.domain.model.VoiceCallRequest;
import FST.MST_RSI.PFA.notification.domain.port.VoiceCallPort;
import FST.MST_RSI.PFA.voicemessage.domain.model.TtsAudio;
import FST.MST_RSI.PFA.voicemessage.domain.port.TextToSpeechPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnExpression("${app.notification.voip.enabled:false} && '${app.notification.voip.provider:local}'.equals('local')")
public class LocalVoiceCallAdapter implements VoiceCallPort {

    private static final Logger log = LoggerFactory.getLogger(LocalVoiceCallAdapter.class);

    private final TextToSpeechPort textToSpeechPort;

    public LocalVoiceCallAdapter(TextToSpeechPort textToSpeechPort) {
        this.textToSpeechPort = textToSpeechPort;
    }

    @Override
    public NotificationDeliveryResult call(VoiceCallRequest request) {
        Optional<TtsAudio> audio = request.liveConversation()
                ? Optional.empty()
                : textToSpeechPort.synthesize(request.message());
        String callId = UUID.randomUUID().toString();

        log.info(
                "VOIP local {} call initiated callId={} alertId={} extension={} recipient={} ttsBytes={}",
                request.liveConversation() ? "manual" : "auto",
                callId,
                request.alertId(),
                request.phoneNumber(),
                request.recipientName(),
                audio.map(a -> a.content().length).orElse(0)
        );

        return NotificationDeliveryResult.sent(callId);
    }
}
