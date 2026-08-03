package FST.MST_RSI.PFA.alerting.domain.event;

import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.common.event.DomainEvent;

public class AlertReceivedEvent extends DomainEvent {

    private final AlertId alertId;
    private final String externalProblemId;
    private final String title;
    private final String applicationName;

    public AlertReceivedEvent(AlertId alertId, String externalProblemId, String title, String applicationName) {
        this.alertId = alertId;
        this.externalProblemId = externalProblemId;
        this.title = title;
        this.applicationName = applicationName;
    }

    public AlertId getAlertId() {
        return alertId;
    }

    public String getExternalProblemId() {
        return externalProblemId;
    }

    public String getTitle() {
        return title;
    }

    public String getApplicationName() {
        return applicationName;
    }
}
