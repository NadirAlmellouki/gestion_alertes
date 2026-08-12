package FST.MST_RSI.PFA.alerting.domain.model;

import java.time.Instant;
import java.util.UUID;

public class AlertTimelineEntry {

    private UUID id;
    private final TimelineEventType eventType;
    private final String message;
    private final Instant occurredAt;

    public AlertTimelineEntry(TimelineEventType eventType, String message, Instant occurredAt) {
        this.eventType = eventType;
        this.message = message;
        this.occurredAt = occurredAt;
    }

    public AlertTimelineEntry(UUID id, TimelineEventType eventType, String message, Instant occurredAt) {
        this.id = id;
        this.eventType = eventType;
        this.message = message;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
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
