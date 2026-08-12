package FST.MST_RSI.PFA.classification.domain.event;

import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationCategory;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationStatus;
import FST.MST_RSI.PFA.common.domain.vo.Confidence;
import FST.MST_RSI.PFA.common.event.DomainEvent;

public class AlertClassifiedEvent extends DomainEvent {

    private final AlertId alertId;
    private final ClassificationCategory category;
    private final Confidence confidence;
    private final String matchedSolution;
    private final String resolvedPsi;
    private final ClassificationStatus status;
    private final String promptVersion;
    private final String provider;
    private final long durationMillis;

    public AlertClassifiedEvent(
            AlertId alertId,
            ClassificationCategory category,
            Confidence confidence,
            String matchedSolution,
            String resolvedPsi,
            ClassificationStatus status,
            String promptVersion,
            String provider,
            long durationMillis
    ) {
        this.alertId = alertId;
        this.category = category;
        this.confidence = confidence;
        this.matchedSolution = matchedSolution;
        this.resolvedPsi = resolvedPsi;
        this.status = status;
        this.promptVersion = promptVersion;
        this.provider = provider;
        this.durationMillis = durationMillis;
    }

    public AlertId getAlertId() {
        return alertId;
    }

    public ClassificationCategory getCategory() {
        return category;
    }

    public Confidence getConfidence() {
        return confidence;
    }

    public String getMatchedSolution() {
        return matchedSolution;
    }

    public String getResolvedPsi() {
        return resolvedPsi;
    }

    public ClassificationStatus getStatus() {
        return status;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getProvider() {
        return provider;
    }

    public long getDurationMillis() {
        return durationMillis;
    }
}
