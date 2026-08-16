package FST.MST_RSI.PFA.notification.infrastructure.sms;

import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryResult;
import FST.MST_RSI.PFA.notification.domain.model.SmsNotificationRequest;
import FST.MST_RSI.PFA.notification.domain.port.SmsNotificationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.notification.sms.enabled", havingValue = "false")
public class FallbackSmsNotificationSenderAdapter implements SmsNotificationPort {

    @Override
    public NotificationDeliveryResult send(SmsNotificationRequest request) {
        return NotificationDeliveryResult.failed("SMS Kafka producer is not configured");
    }
}
