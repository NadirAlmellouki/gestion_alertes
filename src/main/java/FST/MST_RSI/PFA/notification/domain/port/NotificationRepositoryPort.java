package FST.MST_RSI.PFA.notification.domain.port;

import FST.MST_RSI.PFA.notification.domain.model.NotificationRecord;
import FST.MST_RSI.PFA.notification.domain.model.NotificationStatus;
import FST.MST_RSI.PFA.notification.domain.model.NotificationType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepositoryPort {

    default NotificationRecord createPending(
            UUID alertId,
            UUID routingExecutionId,
            NotificationType type,
            UUID recipientPersonId,
            String destination
    ) {
        return createPending(alertId, routingExecutionId, type, recipientPersonId, destination, "AUTO");
    }

    default NotificationRecord createPending(
            UUID alertId,
            UUID routingExecutionId,
            NotificationType type,
            UUID recipientPersonId,
            String destination,
            String callMode
    ) {
        return createPending(alertId, routingExecutionId, type, recipientPersonId, destination, callMode, null);
    }

    NotificationRecord createPending(
            UUID alertId,
            UUID routingExecutionId,
            NotificationType type,
            UUID recipientPersonId,
            String destination,
            String callMode,
            UUID triggeredByPersonId
    );

    void recordAttempt(
            UUID notificationId,
            int attemptNumber,
            String provider,
            NotificationStatus attemptStatus,
            String providerMessageId,
            String errorMessage
    );

    void updateStatus(UUID notificationId, NotificationStatus status);

    Optional<NotificationRecord> findById(UUID notificationId);

    List<NotificationRecord> findByAlertId(UUID alertId);
}
