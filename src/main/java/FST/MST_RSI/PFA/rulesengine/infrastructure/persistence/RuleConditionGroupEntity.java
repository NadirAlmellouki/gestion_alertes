package FST.MST_RSI.PFA.rulesengine.infrastructure.persistence;

import FST.MST_RSI.PFA.rulesengine.domain.model.ConditionBlockType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rule_condition_group")
public class RuleConditionGroupEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private BusinessRuleEntity rule;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 10)
    private ConditionBlockType blockType = ConditionBlockType.CONDITION;

    @Column(name = "logical_operator", nullable = false, length = 5)
    private String logicalOperator = "AND";

    @Column(name = "execution_order", nullable = false)
    private int executionOrder;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("conditionOrder ASC")
    private List<RuleConditionEntity> conditions = new ArrayList<>();

    public RuleConditionGroupEntity() {
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

    public ConditionBlockType getBlockType() {
        return blockType;
    }

    public void setBlockType(ConditionBlockType blockType) {
        this.blockType = blockType;
    }

    public String getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(String logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    public int getExecutionOrder() {
        return executionOrder;
    }

    public void setExecutionOrder(int executionOrder) {
        this.executionOrder = executionOrder;
    }

    public List<RuleConditionEntity> getConditions() {
        return conditions;
    }

    public void setConditions(List<RuleConditionEntity> conditions) {
        this.conditions = conditions;
    }
}
