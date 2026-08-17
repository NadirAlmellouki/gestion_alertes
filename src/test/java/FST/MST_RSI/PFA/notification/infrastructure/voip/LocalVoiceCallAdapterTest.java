package FST.MST_RSI.PFA.notification.infrastructure.voip;

import FST.MST_RSI.PFA.notification.domain.model.VoiceCallRequest;
import FST.MST_RSI.PFA.voicemessage.domain.port.TextToSpeechPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalVoiceCallAdapterTest {

    @Mock
    private TextToSpeechPort textToSpeechPort;

    @Test
    void returnsSuccessForLocalCall() {
        when(textToSpeechPort.synthesize(anyString())).thenReturn(Optional.empty());
        LocalVoiceCallAdapter adapter = new LocalVoiceCallAdapter(textToSpeechPort);

        var result = adapter.call(new VoiceCallRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "1001",
                "Jane Doe",
                "Alerte test",
                null,
                null,
                "corr-1"
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).isNotBlank();
    }
}
