package FST.MST_RSI.PFA.rulesengine.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "rule_action")
public class RuleActionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private BusinessRuleEntity rule;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "action_value", columnDefinition = "TEXT")
    private String actionValue;

    @Column(name = "execution_order", nullable = false)
    private int executionOrder;

    public RuleActionEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BusinessRuleEntity getRule() {
        return rule;
    }

    public void setRule(BusinessRuleEntity rule) {
        this.rule = rule;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionValue() {
        return actionValue;
    }

    public void setActionValue(String actionValue) {
        this.actionValue = actionValue;
    }

    public int getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(int executionOrder) {
        this.executionOrder = executionOrder;
    }
}
