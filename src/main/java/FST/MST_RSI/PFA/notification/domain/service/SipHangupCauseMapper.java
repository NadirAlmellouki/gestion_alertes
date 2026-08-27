package FST.MST_RSI.PFA.notification.domain.service;

import FST.MST_RSI.PFA.notification.domain.model.VoiceCallOutcome;

public final class SipHangupCauseMapper {

    private SipHangupCauseMapper() {
    }

    public static VoiceCallOutcome fromCause(Integer cause, boolean answered) {
        if (cause == null) {
            return answered ? VoiceCallOutcome.HANGUP : VoiceCallOutcome.FAILED;
        }
        return switch (cause) {
            case 17 -> VoiceCallOutcome.BUSY;
            case 21, 22 -> VoiceCallOutcome.REJECTED;
            case 18, 19, 20 -> VoiceCallOutcome.NO_ANSWER;
            case 16 -> answered ? VoiceCallOutcome.HANGUP : VoiceCallOutcome.NO_ANSWER;
            default -> answered ? VoiceCallOutcome.HANGUP : VoiceCallOutcome.FAILED;
        };
    }
}
