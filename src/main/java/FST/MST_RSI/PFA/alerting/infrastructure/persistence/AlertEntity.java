package FST.MST_RSI.PFA.alerting.infrastructure.persistence;

import FST.MST_RSI.PFA.alerting.domain.model.NotificationState;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "alerts")
public class AlertEntity {

    @Id
    private UUID id;

    @Column(name = "external_problem_id", nullable = false, unique = true)
    private String externalProblemId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "application_name")
    private String applicationName;

    private String environment;

    private String severity;

    private String impact;

    @Column(name = "dynatrace_state")
    private String dynatraceState;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_state", nullable = false)
    private NotificationState notificationState;

    @Column(name = "problem_url", length = 1000)
    private String problemUrl;

    @Column(name = "host_name")
    private String hostName;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "problem_started_at")
    private Instant problemStartedAt;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("occurredAt ASC")
    private List<AlertTimelineEntryEntity> timeline = new ArrayList<>();

    protected AlertEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getExternalProblemId() {
        return externalProblemId;
    }

    public void setExternalProblemId(String externalProblemId) {
        this.externalProblemId = externalProblemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getImpact() {
        return impact;
    }

    public void setImpact(String impact) {
        this.impact = impact;
    }

    public String getDynatraceState() {
        return dynatraceState;
    }

    public void setDynatraceState(String dynatraceState) {
        this.dynatraceState = dynatraceState;
    }

    public NotificationState getNotificationState() {
        return notificationState;
    }

    public void setNotificationState(NotificationState notificationState) {
        this.notificationState = notificationState;
    }

    public String getProblemUrl() {
        return problemUrl;
    }

    public void setProblemUrl(String problemUrl) {
        this.problemUrl = problemUrl;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getProblemStartedAt() {
        return problemStartedAt;
    }

    public void setProblemStartedAt(Instant problemStartedAt) {
        this.problemStartedAt = problemStartedAt;
    }

    public List<AlertTimelineEntryEntity> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<AlertTimelineEntryEntity> timeline) {
        this.timeline = timeline;
    }
}
