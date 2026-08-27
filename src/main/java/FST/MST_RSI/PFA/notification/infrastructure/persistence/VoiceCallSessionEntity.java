package FST.MST_RSI.PFA.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "voice_call_session")
public class VoiceCallSessionEntity {

    @Id
    private UUID id;

    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(name = "alert_id")
    private UUID alertId;

    @Column(name = "routing_execution_id")
    private UUID routingExecutionId;

    @Column(name = "person_id")
    private UUID personId;

    @Column(nullable = false, length = 32)
    private String extension;

    @Column(name = "provider_call_id", length = 128)
    private String providerCallId;

    @Column(name = "sound_name")
    private String soundName;

    @Column(nullable = false, length = 30)
    private String outcome = "INITIATED";

    @Column(name = "hangup_cause")
    private Integer hangupCause;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ringing_at")
    private Instant ringingAt;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "live_conversation", nullable = false)
    private boolean liveConversation;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public void setAlertId(UUID alertId) {
        this.alertId = alertId;
    }

    public UUID getRoutingExecutionId() {
        return routingExecutionId;
    }

    public void setRoutingExecutionId(UUID routingExecutionId) {
        this.routingExecutionId = routingExecutionId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public void setPersonId(UUID personId) {
        this.personId = personId;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getProviderCallId() {
        return providerCallId;
    }

    public void setProviderCallId(String providerCallId) {
        this.providerCallId = providerCallId;
    }

    public String getSoundName() {
        return soundName;
    }

    public void setSoundName(String soundName) {
        this.soundName = soundName;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Integer getHangupCause() {
        return hangupCause;
    }

    public void setHangupCause(Integer hangupCause) {
        this.hangupCause = hangupCause;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getRingingAt() {
        return ringingAt;
    }

    public void setRingingAt(Instant ringingAt) {
        this.ringingAt = ringingAt;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(Instant answeredAt) {
        this.answeredAt = answeredAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public boolean isLiveConversation() {
        return liveConversation;
    }

    public void setLiveConversation(boolean liveConversation) {
        this.liveConversation = liveConversation;
    }
}
