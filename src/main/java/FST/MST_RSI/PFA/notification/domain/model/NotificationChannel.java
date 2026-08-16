package FST.MST_RSI.PFA.notification.domain.model;

public enum NotificationChannel {
    EMAIL,
    SMS,
    VOIP;

    public static NotificationChannel fromRoutingChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return null;
        }
        return switch (channel.trim().toUpperCase()) {
            case "EMAIL" -> EMAIL;
            case "SMS" -> SMS;
            case "VOIP", "VOICE" -> VOIP;
            default -> null;
        };
    }
}
