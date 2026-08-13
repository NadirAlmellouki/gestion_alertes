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
@Table(name = "rule_condition")
public class RuleConditionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private RuleConditionGroupEntity group;

    @Column(name = "field_name", nullable = false, length = 150)
    private String fieldName;

    @Column(nullable = false, length = 30)
    private String operator;

    @Column(name = "expected_value", columnDefinition = "TEXT")
    private String expectedValue;

    @Column(name = "value_type", nullable = false, length = 30)
    private String valueType = "STRING";

    @Column(name = "condition_order", nullable = false)
    private int conditionOrder;

    public RuleConditionEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RuleConditionGroupEntity getGroup() {
        return group;
    }

    public void setGroup(RuleConditionGroupEntity group) {
        this.group = group;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public int getConditionOrder() {
        return conditionOrder;
    }

    public void setConditionOrder(int conditionOrder) {
        this.conditionOrder = conditionOrder;
    }
}
