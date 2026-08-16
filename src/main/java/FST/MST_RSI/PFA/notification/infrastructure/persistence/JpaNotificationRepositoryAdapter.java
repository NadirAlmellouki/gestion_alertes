package FST.MST_RSI.PFA.notification.infrastructure.persistence;

import FST.MST_RSI.PFA.notification.domain.model.NotificationRecord;
import FST.MST_RSI.PFA.notification.domain.model.NotificationStatus;
import FST.MST_RSI.PFA.notification.domain.model.NotificationType;
import FST.MST_RSI.PFA.notification.domain.port.NotificationRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaNotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository notificationRepository;

    public JpaNotificationRepositoryAdapter(NotificationJpaRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public NotificationRecord createPending(
            UUID alertId,
            UUID routingExecutionId,
            NotificationType type,
            UUID recipientPersonId,
            String destination
    ) {
        UUID notificationId = UUID.randomUUID();
        NotificationEntity notification = new NotificationEntity();
        notification.setId(notificationId);
        notification.setAlertId(alertId);
        notification.setRoutingExecutionId(routingExecutionId);
        notification.setNotificationType(type);
        notification.setNotificationStatus(NotificationStatus.PENDING);
        notification.setPriority(0);
        notification.setCreatedAt(Instant.now());

        NotificationRecipientEntity recipient = new NotificationRecipientEntity();
        recipient.setId(UUID.randomUUID());
        recipient.setNotification(notification);
        recipient.setPersonId(recipientPersonId);
        recipient.setChannel(type.name());
        recipient.setDestination(destination);
        recipient.setRecipientOrder(0);
        notification.getRecipients().add(recipient);

        NotificationEntity saved = notificationRepository.save(notification);
        NotificationRecipientEntity savedRecipient = saved.getRecipients().getFirst();
        return toRecord(saved, savedRecipient);
    }

    @Override
    @Transactional
    public void recordAttempt(
            UUID notificationId,
            int attemptNumber,
            String provider,
            NotificationStatus attemptStatus,
            String providerMessageId,
            String errorMessage
    ) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalStateException("Notification not found: " + notificationId));
        NotificationRecipientEntity recipient = notification.getRecipients().getFirst();

        NotificationAttemptEntity attempt = new NotificationAttemptEntity();
        attempt.setId(UUID.randomUUID());
        attempt.setRecipient(recipient);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setProvider(provider);
        attempt.setProviderMessageId(providerMessageId);
        attempt.setStatus(mapAttemptStatus(attemptStatus));
        attempt.setErrorMessage(errorMessage);
        Instant now = Instant.now();
        attempt.setStartedAt(now);
        attempt.setFinishedAt(now);
        recipient.getAttempts().add(attempt);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void updateStatus(UUID notificationId, NotificationStatus status) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalStateException("Notification not found: " + notificationId));
        notification.setNotificationStatus(status);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationRecord> findById(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> !n.getRecipients().isEmpty())
                .map(n -> toRecord(n, n.getRecipients().getFirst()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationRecord> findByAlertId(UUID alertId) {
        return notificationRepository.findByAlertIdOrderByCreatedAtDesc(alertId).stream()
                .filter(n -> !n.getRecipients().isEmpty())
                .map(n -> toRecord(n, n.getRecipients().getFirst()))
                .toList();
    }

    private static NotificationRecord toRecord(NotificationEntity notification, NotificationRecipientEntity recipient) {
        return new NotificationRecord(
                notification.getId(),
                notification.getAlertId(),
                notification.getRoutingExecutionId(),
                notification.getNotificationType(),
                notification.getNotificationStatus(),
                recipient.getPersonId(),
                recipient.getDestination(),
                notification.getCreatedAt()
        );
    }

    private static String mapAttemptStatus(NotificationStatus status) {
        return switch (status) {
            case SENT -> "SENT";
            case FAILED -> "FAILED";
            default -> "PENDING";
        };
    }
}
