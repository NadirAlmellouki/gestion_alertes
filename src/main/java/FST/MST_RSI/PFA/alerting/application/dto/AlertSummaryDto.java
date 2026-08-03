package FST.MST_RSI.PFA.alerting.application.dto;

import FST.MST_RSI.PFA.alerting.domain.model.NotificationState;

import java.time.Instant;

public record AlertSummaryDto(
        String id,
        String externalProblemId,
        String title,
        String applicationName,
        String environment,
        String severity,
        NotificationState notificationState,
        Instant receivedAt
) {
}
