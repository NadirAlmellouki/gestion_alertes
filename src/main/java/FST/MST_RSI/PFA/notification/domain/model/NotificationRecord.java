package FST.MST_RSI.PFA.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public record NotificationRecord(
        UUID id,
        UUID alertId,
        UUID routingExecutionId,
        NotificationType notificationType,
        NotificationStatus notificationStatus,
        UUID recipientPersonId,
        String destination,
        Instant createdAt
) {
}
