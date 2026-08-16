package FST.MST_RSI.PFA.notification.infrastructure.email;

import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryRequest;
import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryResult;
import FST.MST_RSI.PFA.notification.domain.port.EmailNotificationPort;
import FST.MST_RSI.PFA.notification.infrastructure.config.EmailNotificationProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSenderAdapter implements EmailNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSenderAdapter.class);
    private static final String PROVIDER = "smtp";

    private final JavaMailSender mailSender;
    private final EmailNotificationProperties properties;

    public EmailNotificationSenderAdapter(JavaMailSender mailSender, EmailNotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public NotificationDeliveryResult send(NotificationDeliveryRequest request) {
        if (!properties.isEnabled()) {
            return NotificationDeliveryResult.failed("Email notifications are disabled");
        }
        if (request.recipientEmail() == null || request.recipientEmail().isBlank()) {
            return NotificationDeliveryResult.failed("Recipient email is missing");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(properties.getFrom());
            helper.setTo(request.recipientEmail());
            helper.setSubject(request.subject());
            helper.setText(request.body(), false);
            mailSender.send(message);
            String messageId = message.getMessageID();
            log.info("Email sent to {} for alert {}", request.recipientEmail(), request.alertId());
            return NotificationDeliveryResult.sent(messageId);
        } catch (Exception ex) {
            log.error("Failed to send email to {} for alert {}", request.recipientEmail(), request.alertId(), ex);
            return NotificationDeliveryResult.failed(ex.getMessage());
        }
    }

    public String providerName() {
        return PROVIDER;
    }
}
