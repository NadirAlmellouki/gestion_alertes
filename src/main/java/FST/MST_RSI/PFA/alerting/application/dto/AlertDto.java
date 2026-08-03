package FST.MST_RSI.PFA.alerting.application.dto;

import FST.MST_RSI.PFA.alerting.domain.model.NotificationState;

import java.time.Instant;
import java.util.List;

public record AlertDto(
        String id,
        String externalProblemId,
        String title,
        String applicationName,
        String environment,
        String severity,
        String impact,
        String dynatraceState,
        NotificationState notificationState,
        String problemUrl,
        String hostName,
        Instant receivedAt,
        Instant problemStartedAt,
        List<AlertTimelineEntryDto> timeline
) {
}
