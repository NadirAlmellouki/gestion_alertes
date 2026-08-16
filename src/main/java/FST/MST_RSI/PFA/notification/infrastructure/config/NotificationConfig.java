package FST.MST_RSI.PFA.notification.infrastructure.config;

import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryResult;
import FST.MST_RSI.PFA.notification.domain.port.SmsNotificationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({EmailNotificationProperties.class, SmsNotificationProperties.class})
public class NotificationConfig {
}
