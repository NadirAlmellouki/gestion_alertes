package FST.MST_RSI.PFA.routingengine.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "routing_step")
public class RoutingStepEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routing_policy_id", nullable = false)
    private RoutingPolicyEntity policy;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;

    @Column(name = "target_role", nullable = false, length = 40)
    private String targetRole;

    @Column(name = "target_unit_type", nullable = false, length = 20)
    private String targetUnitType;

    @Column(length = 20)
    private String channel;

    @Column(name = "delay_after_seconds", nullable = false)
    private int delayAfterSeconds;

    public RoutingStepEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RoutingPolicyEntity getPolicy() {
        return policy;
    }

    public void setPolicy(RoutingPolicyEntity policy) {
        this.policy = policy;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(int stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getTargetUnitType() {
        return targetUnitType;
    }

    public void setTargetUnitType(String targetUnitType) {
        this.targetUnitType = targetUnitType;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public int getDelayAfterSeconds() {
        return delayAfterSeconds;
    }

    public void setDelayAfterSeconds(int delayAfterSeconds) {
        this.delayAfterSeconds = delayAfterSeconds;
    }
}
