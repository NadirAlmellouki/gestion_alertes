package FST.MST_RSI.PFA.notification.domain.service;

import FST.MST_RSI.PFA.notification.domain.model.VoiceCallOutcome;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SipHangupCauseMapperTest {

    @Test
    void mapsSipCauses() {
        assertThat(SipHangupCauseMapper.fromCause(17, false)).isEqualTo(VoiceCallOutcome.BUSY);
        assertThat(SipHangupCauseMapper.fromCause(21, false)).isEqualTo(VoiceCallOutcome.REJECTED);
        assertThat(SipHangupCauseMapper.fromCause(19, false)).isEqualTo(VoiceCallOutcome.NO_ANSWER);
        assertThat(SipHangupCauseMapper.fromCause(16, true)).isEqualTo(VoiceCallOutcome.HANGUP);
        assertThat(SipHangupCauseMapper.fromCause(16, false)).isEqualTo(VoiceCallOutcome.NO_ANSWER);
    }
}
