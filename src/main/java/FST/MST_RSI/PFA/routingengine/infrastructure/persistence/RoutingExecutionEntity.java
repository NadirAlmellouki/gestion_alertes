package FST.MST_RSI.PFA.routingengine.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "routing_execution")
public class RoutingExecutionEntity {

    @Id
    private UUID id;

    @Column(name = "alert_id", nullable = false)
    private UUID alertId;

    @Column(name = "classification_id")
    private UUID classificationId;

    @Column(name = "routing_policy_id")
    private UUID routingPolicyId;

    @Column(name = "selected_solution_id")
    private UUID selectedSolutionId;

    @Column(name = "selected_person_id")
    private UUID selectedPersonId;

    @Column(name = "current_step", nullable = false)
    private int currentStep;

    @Column(name = "routing_status", nullable = false, length = 30)
    private String routingStatus;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected RoutingExecutionEntity() {
    }

    public static RoutingExecutionEntity create(
            UUID id,
            UUID alertId,
            UUID classificationId,
            UUID routingPolicyId,
            UUID selectedSolutionId,
            UUID selectedPersonId,
            int currentStep,
            String routingStatus,
            Instant startedAt
    ) {
        RoutingExecutionEntity entity = new RoutingExecutionEntity();
        entity.id = id;
        entity.alertId = alertId;
        entity.classificationId = classificationId;
        entity.routingPolicyId = routingPolicyId;
        entity.selectedSolutionId = selectedSolutionId;
        entity.selectedPersonId = selectedPersonId;
        entity.currentStep = currentStep;
        entity.routingStatus = routingStatus;
        entity.startedAt = startedAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public void setRoutingStatus(String routingStatus) {
        this.routingStatus = routingStatus;
    }

    public void setSelectedPersonId(UUID selectedPersonId) {
        this.selectedPersonId = selectedPersonId;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }
}
