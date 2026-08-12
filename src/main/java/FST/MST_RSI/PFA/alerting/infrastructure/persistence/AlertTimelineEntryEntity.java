package FST.MST_RSI.PFA.alerting.infrastructure.persistence;

import FST.MST_RSI.PFA.alerting.domain.model.TimelineEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert_timeline")
public class AlertTimelineEntryEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private AlertEntity alert;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 100)
    private TimelineEventType eventType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "created_by")
    private String createdBy;

    protected AlertTimelineEntryEntity() {
    }

    public AlertTimelineEntryEntity(
            AlertEntity alert,
            TimelineEventType eventType,
            String description,
            Instant eventTime
    ) {
        this.id = UUID.randomUUID();
        this.alert = alert;
        this.eventType = eventType;
        this.description = description;
        this.eventTime = eventTime;
    }

    public UUID getId() {
        return id;
    }

    public AlertEntity getAlert() {
        return alert;
    }

    public TimelineEventType getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
