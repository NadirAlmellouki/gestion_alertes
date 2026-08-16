package FST.MST_RSI.PFA.notification.domain.model;

public record NotificationDeliveryResult(
        boolean success,
        String providerMessageId,
        String errorMessage
) {
    public static NotificationDeliveryResult sent(String providerMessageId) {
        return new NotificationDeliveryResult(true, providerMessageId, null);
    }

    public static NotificationDeliveryResult failed(String errorMessage) {
        return new NotificationDeliveryResult(false, null, errorMessage);
    }
}
