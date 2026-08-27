package FST.MST_RSI.PFA.voicemessage.infrastructure.tts;

import FST.MST_RSI.PFA.voicemessage.domain.model.TtsAudio;
import FST.MST_RSI.PFA.voicemessage.domain.port.TextToSpeechPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "app.voice.tts", name = "provider", havingValue = "fake", matchIfMissing = true)
public class FakeTtsAdapter implements TextToSpeechPort {

    @Override
    public Optional<TtsAudio> synthesize(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(PcmWavTone.shortAlertTone());
    }
}
