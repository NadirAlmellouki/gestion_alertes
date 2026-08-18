package FST.MST_RSI.PFA.audit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    private UUID id;

    @Column(name = "actor_person_id")
    private UUID actorPersonId;

    @Column(name = "alert_id")
    private UUID alertId;

    @Column(name = "llm_analysis_id")
    private UUID llmAnalysisId;

    @Column(name = "routing_execution_id")
    private UUID routingExecutionId;

    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_name", length = 100)
    private String entityName;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AuditLogEntity() {
    }

    public static AuditLogEntity create(
            UUID id,
            UUID actorPersonId,
            UUID alertId,
            UUID llmAnalysisId,
            UUID routingExecutionId,
            UUID notificationId,
            String action,
            String entityName,
            UUID entityId,
            String description,
            String correlationId,
            String ipAddress,
            Instant createdAt
    ) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.id = id;
        entity.actorPersonId = actorPersonId;
        entity.alertId = alertId;
        entity.llmAnalysisId = llmAnalysisId;
        entity.routingExecutionId = routingExecutionId;
        entity.notificationId = notificationId;
        entity.action = action;
        entity.entityName = entityName;
        entity.entityId = entityId;
        entity.description = description;
        entity.correlationId = correlationId;
        entity.ipAddress = ipAddress;
        entity.createdAt = createdAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public String getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getLlmAnalysisId() {
        return llmAnalysisId;
    }

    public UUID getRoutingExecutionId() {
        return routingExecutionId;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public String getEntityName() {
        return entityName;
    }

    public UUID getEntityId() {
        return entityId;
    }
}
