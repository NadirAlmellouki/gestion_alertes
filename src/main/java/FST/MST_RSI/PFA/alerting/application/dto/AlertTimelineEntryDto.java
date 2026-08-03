package FST.MST_RSI.PFA.alerting.application.dto;

import FST.MST_RSI.PFA.alerting.domain.model.NotificationState;
import FST.MST_RSI.PFA.alerting.domain.model.TimelineEventType;

import java.time.Instant;

public record AlertTimelineEntryDto(
        Long id,
        TimelineEventType eventType,
        String message,
        Instant occurredAt
) {
}
