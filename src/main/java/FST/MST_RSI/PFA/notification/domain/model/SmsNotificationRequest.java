package FST.MST_RSI.PFA.notification.domain.model;

import java.util.UUID;

public record SmsNotificationRequest(
        UUID alertId,
        UUID routingExecutionId,
        UUID recipientPersonId,
        String recipientPhone,
        String recipientName,
        String applicationName,
        String alertTitle,
        String alertSeverity,
        String problemId,
        String matchedSolution,
        String correlationId
) {
}
