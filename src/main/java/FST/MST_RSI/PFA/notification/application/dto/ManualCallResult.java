package FST.MST_RSI.PFA.notification.application.dto;

import java.util.UUID;

public record ManualCallResult(
        String status,
        String personName,
        String destination,
        String providerMessageId,
        String detail,
        UUID notificationId,
        UUID alertId,
        boolean liveConversation
) {
}
