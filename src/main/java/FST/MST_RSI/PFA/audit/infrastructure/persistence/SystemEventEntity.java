package FST.MST_RSI.PFA.audit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_event")
public class SystemEventEntity {

    @Id
    private UUID id;

    @Column(name = "alert_id")
    private UUID alertId;

    @Column(name = "llm_analysis_id")
    private UUID llmAnalysisId;

    @Column(name = "source_module", nullable = false, length = 100)
    private String sourceModule;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SystemEventEntity() {
    }

    public static SystemEventEntity create(
            UUID id,
            UUID alertId,
            UUID llmAnalysisId,
            String sourceModule,
            String severity,
            String eventType,
            String message,
            String correlationId,
            Instant createdAt
    ) {
        SystemEventEntity entity = new SystemEventEntity();
        entity.id = id;
        entity.alertId = alertId;
        entity.llmAnalysisId = llmAnalysisId;
        entity.sourceModule = sourceModule;
        entity.severity = severity;
        entity.eventType = eventType;
        entity.message = message;
        entity.correlationId = correlationId;
        entity.createdAt = createdAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public String getSourceModule() {
        return sourceModule;
    }

    public String getSeverity() {
        return severity;
    }

    public String getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
