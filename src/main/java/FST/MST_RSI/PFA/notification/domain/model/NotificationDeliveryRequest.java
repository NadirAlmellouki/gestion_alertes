package FST.MST_RSI.PFA.notification.domain.model;

import java.util.UUID;

public record NotificationDeliveryRequest(
        UUID alertId,
        UUID routingExecutionId,
        String recipientEmail,
        String recipientName,
        UUID recipientPersonId,
        String subject,
        String body,
        String correlationId
) {
}
