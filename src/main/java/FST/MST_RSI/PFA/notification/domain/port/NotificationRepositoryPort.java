package FST.MST_RSI.PFA.notification.domain.port;

import FST.MST_RSI.PFA.notification.domain.model.NotificationRecord;
import FST.MST_RSI.PFA.notification.domain.model.NotificationStatus;
import FST.MST_RSI.PFA.notification.domain.model.NotificationType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepositoryPort {

    NotificationRecord createPending(
            UUID alertId,
            UUID routingExecutionId,
            NotificationType type,
            UUID recipientPersonId,
            String destination
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
