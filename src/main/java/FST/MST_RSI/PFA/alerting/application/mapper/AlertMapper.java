package FST.MST_RSI.PFA.alerting.application.mapper;

import FST.MST_RSI.PFA.alerting.application.dto.AlertDto;
import FST.MST_RSI.PFA.alerting.application.dto.AlertSummaryDto;
import FST.MST_RSI.PFA.alerting.application.dto.AlertTimelineEntryDto;
import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.model.AlertTimelineEntry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlertMapper {

    public AlertSummaryDto toSummary(Alert alert) {
        return new AlertSummaryDto(
                alert.getId().value().toString(),
                alert.getExternalProblemId(),
                alert.getTitle(),
                alert.getApplicationName(),
                alert.getEnvironment(),
                alert.getSeverity(),
                alert.getNotificationState(),
                alert.getReceivedAt()
        );
    }

    public List<AlertSummaryDto> toSummaryList(List<Alert> alerts) {
        return alerts.stream().map(this::toSummary).toList();
    }

    public AlertDto toDetail(Alert alert) {
        return new AlertDto(
                alert.getId().value().toString(),
                alert.getExternalProblemId(),
                alert.getTitle(),
                alert.getApplicationName(),
                alert.getEnvironment(),
                alert.getSeverity(),
                alert.getImpact(),
                alert.getDynatraceState(),
                alert.getNotificationState(),
                alert.getProblemUrl(),
                alert.getHostName(),
                alert.getReceivedAt(),
                alert.getProblemStartedAt(),
                alert.getTimeline().stream().map(this::toTimelineEntry).toList()
        );
    }

    private AlertTimelineEntryDto toTimelineEntry(AlertTimelineEntry entry) {
        return new AlertTimelineEntryDto(
                entry.getId() == null ? null : entry.getId().toString(),
                entry.getEventType(),
                entry.getMessage(),
                entry.getOccurredAt()
        );
    }
}
