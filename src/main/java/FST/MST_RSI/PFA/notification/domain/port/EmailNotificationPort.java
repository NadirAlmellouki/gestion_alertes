package FST.MST_RSI.PFA.notification.domain.port;

import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryRequest;
import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryResult;

public interface EmailNotificationPort {

    NotificationDeliveryResult send(NotificationDeliveryRequest request);
}
