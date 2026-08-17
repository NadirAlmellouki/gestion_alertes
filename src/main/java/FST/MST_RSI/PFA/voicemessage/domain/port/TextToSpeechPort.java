package FST.MST_RSI.PFA.voicemessage.domain.port;

import FST.MST_RSI.PFA.voicemessage.domain.model.TtsAudio;

import java.util.Optional;

public interface TextToSpeechPort {

    Optional<TtsAudio> synthesize(String text);
}
