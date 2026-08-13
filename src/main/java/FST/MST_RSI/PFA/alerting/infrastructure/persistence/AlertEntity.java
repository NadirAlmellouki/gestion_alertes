package FST.MST_RSI.PFA.alerting.infrastructure.persistence;

import FST.MST_RSI.PFA.alerting.domain.model.NotificationState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert")
public class AlertEntity {

    @Id
    private UUID id;

    @Column(name = "problem_id", nullable = false, unique = true, length = 100)
    private String problemId;

    @Column(name = "display_id", length = 100)
    private String displayId;

    @Column(length = 500)
    private String title;

    @Column(length = 20)
    private String severity;

    @Column(name = "impact_level", length = 50)
    private String impactLevel;

    @Column(length = 20)
    private String status;

    @Column(nullable = false, length = 50)
    private String source = "DYNATRACE";

    @Column(name = "linked_problem_id", length = 100)
    private String linkedProblemId;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private String rawPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entity_tags", columnDefinition = "jsonb")
    private String entityTags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "impact_analysis", columnDefinition = "jsonb")
    private String impactAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_details", columnDefinition = "jsonb")
    private String evidenceDetails;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_state", nullable = false, length = 30)
    private NotificationState notificationState = NotificationState.EN_ATTENTE;

    protected AlertEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProblemId() {
        return problemId;
    }

    public void setProblemId(String problemId) {
        this.problemId = problemId;
    }

    public String getDisplayId() {
        return displayId;
    }

    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getImpactLevel() {
        return impactLevel;
    }

    public void setImpactLevel(String impactLevel) {
        this.impactLevel = impactLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLinkedProblemId() {
        return linkedProblemId;
    }

    public void setLinkedProblemId(String linkedProblemId) {
        this.linkedProblemId = linkedProblemId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public String getEntityTags() {
        return entityTags;
    }

    public void setEntityTags(String entityTags) {
        this.entityTags = entityTags;
    }

    public String getImpactAnalysis() {
        return impactAnalysis;
    }

    public void setImpactAnalysis(String impactAnalysis) {
        this.impactAnalysis = impactAnalysis;
    }

    public String getEvidenceDetails() {
        return evidenceDetails;
    }

    public void setEvidenceDetails(String evidenceDetails) {
        this.evidenceDetails = evidenceDetails;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public NotificationState getNotificationState() {
        return notificationState;
    }

    public void setNotificationState(NotificationState notificationState) {
        this.notificationState = notificationState;
    }
}
