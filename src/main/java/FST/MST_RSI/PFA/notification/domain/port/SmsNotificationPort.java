package FST.MST_RSI.PFA.notification.domain.port;

import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryResult;
import FST.MST_RSI.PFA.notification.domain.model.SmsNotificationRequest;

public interface SmsNotificationPort {

    NotificationDeliveryResult send(SmsNotificationRequest request);
}
