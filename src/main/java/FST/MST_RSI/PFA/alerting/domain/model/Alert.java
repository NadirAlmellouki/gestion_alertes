package FST.MST_RSI.PFA.alerting.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final List<AlertTimelineEntry> timeline;

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
            Instant problemStartedAt,
            List<AlertTimelineEntry> timeline
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
        this.timeline = new ArrayList<>(timeline);
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
        Alert alert = new Alert(
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
                problemStartedAt,
                new ArrayList<>()
        );
        alert.addTimelineEntry(TimelineEventType.RECEIVED, "Alerte reçue depuis Dynatrace");
        return alert;
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
            Instant problemStartedAt,
            List<AlertTimelineEntry> timeline
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
                problemStartedAt,
                timeline
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
        addTimelineEntry(TimelineEventType.UPDATED, "Mise à jour reçue depuis Dynatrace");
    }

    public void addTimelineEntry(TimelineEventType eventType, String message) {
        timeline.add(new AlertTimelineEntry(eventType, message, Instant.now()));
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

    public List<AlertTimelineEntry> getTimeline() {
        return Collections.unmodifiableList(timeline);
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
