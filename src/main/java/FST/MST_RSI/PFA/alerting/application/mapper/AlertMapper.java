package FST.MST_RSI.PFA.alerting.application.mapper;

import FST.MST_RSI.PFA.alerting.application.dto.AlertDto;
import FST.MST_RSI.PFA.alerting.application.dto.AlertSummaryDto;
import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.classification.infrastructure.persistence.AlertLlmAnalysisEntity;
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
        return toDetail(alert, null);
    }

    public AlertDto toDetail(Alert alert, AlertLlmAnalysisEntity analysis) {
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
                analysis == null || analysis.getStatus() == null ? null : analysis.getStatus().name(),
                analysis == null || analysis.getCategory() == null ? null : analysis.getCategory().name(),
                analysis == null ? null : analysis.getProblemType(),
                analysis == null || analysis.getConfidence() == null ? null : analysis.getConfidence().doubleValue(),
                analysis == null ? null : analysis.isRequiresHumanValidation(),
                analysis == null ? null : analysis.getMatchedSolution(),
                analysis == null ? null : analysis.getMatchedDomain(),
                analysis == null ? null : analysis.getMatchedPole(),
                analysis == null ? null : analysis.getMatchedEntity(),
                analysis == null ? null : analysis.getResolvedPsi(),
                analysis == null ? null : analysis.getSummary(),
                analysis == null ? null : analysis.getProbableCause(),
                analysis == null ? null : analysis.getJustification()
        );
    }
}
