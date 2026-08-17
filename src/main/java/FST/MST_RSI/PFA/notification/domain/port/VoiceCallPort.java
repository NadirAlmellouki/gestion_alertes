package FST.MST_RSI.PFA.notification.domain.port;

import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryResult;
import FST.MST_RSI.PFA.notification.domain.model.VoiceCallRequest;

public interface VoiceCallPort {

    NotificationDeliveryResult call(VoiceCallRequest request);
}
