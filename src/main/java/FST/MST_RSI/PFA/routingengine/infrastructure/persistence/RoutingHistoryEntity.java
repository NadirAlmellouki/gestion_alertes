package FST.MST_RSI.PFA.routingengine.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "routing_history")
public class RoutingHistoryEntity {

    @Id
    private UUID id;

    @Column(name = "routing_execution_id", nullable = false)
    private UUID routingExecutionId;

    @Column(name = "routing_step_id")
    private UUID routingStepId;

    @Column(name = "target_person_id")
    private UUID targetPersonId;

    @Column(name = "target_unit_id")
    private UUID targetUnitId;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(name = "action_time", nullable = false)
    private Instant actionTime;

    @Column(columnDefinition = "TEXT")
    private String details;

    public RoutingHistoryEntity() {
    }

    public static RoutingHistoryEntity create(
            UUID id,
            UUID routingExecutionId,
            UUID routingStepId,
            UUID targetPersonId,
            UUID targetUnitId,
            String action,
            String details,
            Instant actionTime
    ) {
        RoutingHistoryEntity entity = new RoutingHistoryEntity();
        entity.id = id;
        entity.routingExecutionId = routingExecutionId;
        entity.routingStepId = routingStepId;
        entity.targetPersonId = targetPersonId;
        entity.targetUnitId = targetUnitId;
        entity.action = action;
        entity.details = details;
        entity.actionTime = actionTime;
        return entity;
    }
}
