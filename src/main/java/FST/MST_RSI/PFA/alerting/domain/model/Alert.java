package FST.MST_RSI.PFA.alerting.domain.model;

import java.time.Instant;
import java.util.Objects;

public class Alert {

    private final AlertId id;
    private final String externalProblemId;
    private String title;
    private String applicationName;
    private String environment;
    private String severity;
    private String impact;
    private String dynatraceState;
    private NotificationState notificationState;
    private String problemUrl;
    private String hostName;
    private String rawPayload;
    private final Instant receivedAt;
    private Instant problemStartedAt;

    private Alert(
            AlertId id,
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
            String rawPayload,
            Instant receivedAt,
            Instant problemStartedAt
    ) {
        this.id = id;
        this.externalProblemId = externalProblemId;
        this.title = title;
        this.applicationName = applicationName;
        this.environment = environment;
        this.severity = severity;
        this.impact = impact;
        this.dynatraceState = dynatraceState;
        this.notificationState = notificationState;
        this.problemUrl = problemUrl;
        this.hostName = hostName;
        this.rawPayload = rawPayload;
        this.receivedAt = receivedAt;
        this.problemStartedAt = problemStartedAt;
    }

    public static Alert createNew(
            String externalProblemId,
            String title,
            String applicationName,
            String environment,
            String severity,
            String impact,
            String dynatraceState,
            String problemUrl,
            String hostName,
            String rawPayload,
            Instant problemStartedAt
    ) {
        Instant now = Instant.now();
        return new Alert(
                AlertId.generate(),
                externalProblemId,
                title,
                applicationName,
                environment,
                severity,
                impact,
                dynatraceState,
                NotificationState.EN_ATTENTE,
                problemUrl,
                hostName,
                rawPayload,
                now,
                problemStartedAt
        );
    }

    public static Alert restore(
            AlertId id,
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
            String rawPayload,
            Instant receivedAt,
            Instant problemStartedAt
    ) {
        return new Alert(
                id,
                externalProblemId,
                title,
                applicationName,
                environment,
                severity,
                impact,
                dynatraceState,
                notificationState,
                problemUrl,
                hostName,
                rawPayload,
                receivedAt,
                problemStartedAt
        );
    }

    public void updateFromDynatrace(
            String title,
            String applicationName,
            String environment,
            String severity,
            String impact,
            String dynatraceState,
            String problemUrl,
            String hostName,
            String rawPayload,
            Instant problemStartedAt
    ) {
        this.title = title;
        this.applicationName = applicationName;
        this.environment = environment;
        this.severity = severity;
        this.impact = impact;
        this.dynatraceState = dynatraceState;
        this.problemUrl = problemUrl;
        this.hostName = hostName;
        this.rawPayload = rawPayload;
        this.problemStartedAt = problemStartedAt;
    }

    public AlertId getId() {
        return id;
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

    public String getEnvironment() {
        return environment;
    }

    public String getSeverity() {
        return severity;
    }

    public String getImpact() {
        return impact;
    }

    public String getDynatraceState() {
        return dynatraceState;
    }

    public NotificationState getNotificationState() {
        return notificationState;
    }

    public void markNotificationInProgress() {
        this.notificationState = NotificationState.EN_COURS;
    }

    public void markNotificationSent() {
        this.notificationState = NotificationState.ENVOYEE;
    }

    public void markNotificationFailed() {
        this.notificationState = NotificationState.ECHOUEE;
    }

    public String getProblemUrl() {
        return problemUrl;
    }

    public String getHostName() {
        return hostName;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getProblemStartedAt() {
        return problemStartedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Alert alert)) {
            return false;
        }
        return Objects.equals(id, alert.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
