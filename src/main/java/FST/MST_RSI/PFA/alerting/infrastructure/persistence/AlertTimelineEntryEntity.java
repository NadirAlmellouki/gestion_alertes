package FST.MST_RSI.PFA.alerting.infrastructure.persistence;

import FST.MST_RSI.PFA.alerting.domain.model.TimelineEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "alert_timeline")
public class AlertTimelineEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id", nullable = false)
    private AlertEntity alert;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private TimelineEventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AlertTimelineEntryEntity() {
    }

    public AlertTimelineEntryEntity(AlertEntity alert, TimelineEventType eventType, String message, Instant occurredAt) {
        this.alert = alert;
        this.eventType = eventType;
        this.message = message;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public AlertEntity getAlert() {
        return alert;
    }

    public TimelineEventType getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
