package FST.MST_RSI.PFA.rulesengine.application.mapper;

import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRule;
import FST.MST_RSI.PFA.rulesengine.domain.model.ConditionBlockType;
import FST.MST_RSI.PFA.rulesengine.domain.model.ConditionOperator;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleAction;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleCondition;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleConditionGroup;
import FST.MST_RSI.PFA.rulesengine.infrastructure.persistence.BusinessRuleEntity;
import FST.MST_RSI.PFA.rulesengine.infrastructure.persistence.RuleActionEntity;
import FST.MST_RSI.PFA.rulesengine.infrastructure.persistence.RuleConditionEntity;
import FST.MST_RSI.PFA.rulesengine.infrastructure.persistence.RuleConditionGroupEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BusinessRuleMapper {

    public BusinessRule toDomain(BusinessRuleEntity entity) {
        List<RuleConditionGroup> groups = entity.getConditionGroups().stream()
                .map(g -> new RuleConditionGroup(
                        g.getId(),
                        g.getBlockType(),
                        g.getLogicalOperator(),
                        g.getExecutionOrder(),
                        g.getConditions().stream()
                                .map(c -> new RuleCondition(
                                        c.getId(),
                                        c.getFieldName(),
                                        ConditionOperator.fromDb(c.getOperator()),
                                        c.getExpectedValue(),
                                        c.getValueType(),
                                        c.getConditionOrder()
                                ))
                                .toList()
                ))
                .toList();

        List<RuleAction> actions = entity.getActions().stream()
                .map(a -> new RuleAction(a.getId(), a.getActionType(), a.getActionValue(), a.getExecutionOrder()))
                .toList();

        return new BusinessRule(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getEvaluationPriority(),
                entity.isEnabled(),
                entity.isStopOnMatch(),
                entity.getRuleOrigin(),
                groups,
                actions
        );
    }

    public void mapToEntity(BusinessRule rule, BusinessRuleEntity entity) {
        entity.setId(rule.id());
        entity.setCode(rule.code());
        entity.setName(rule.name());
        entity.setDescription(rule.description());
        entity.setEvaluationPriority(rule.evaluationPriority());
        entity.setEnabled(rule.enabled());
        entity.setStopOnMatch(rule.stopOnMatch());
        entity.setRuleOrigin(rule.origin());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(java.time.Instant.now());
        }

        entity.getConditionGroups().clear();
        for (RuleConditionGroup group : rule.conditionGroups()) {
            RuleConditionGroupEntity groupEntity = new RuleConditionGroupEntity();
            groupEntity.setId(group.id() != null ? group.id() : java.util.UUID.randomUUID());
            groupEntity.setRule(entity);
            groupEntity.setBlockType(group.blockType());
            groupEntity.setLogicalOperator(group.logicalOperator());
            groupEntity.setExecutionOrder(group.executionOrder());
            for (RuleCondition condition : group.conditions()) {
                RuleConditionEntity conditionEntity = new RuleConditionEntity();
                conditionEntity.setId(condition.id() != null ? condition.id() : java.util.UUID.randomUUID());
                conditionEntity.setGroup(groupEntity);
                conditionEntity.setFieldName(condition.fieldName());
                conditionEntity.setOperator(condition.operator().toDbValue());
                conditionEntity.setExpectedValue(condition.expectedValue());
                conditionEntity.setValueType(condition.valueType());
                conditionEntity.setConditionOrder(condition.conditionOrder());
                groupEntity.getConditions().add(conditionEntity);
            }
            entity.getConditionGroups().add(groupEntity);
        }

        entity.getActions().clear();
        for (RuleAction action : rule.actions()) {
            RuleActionEntity actionEntity = new RuleActionEntity();
            actionEntity.setId(action.id() != null ? action.id() : java.util.UUID.randomUUID());
            actionEntity.setRule(entity);
            actionEntity.setActionType(action.actionType());
            actionEntity.setActionValue(action.actionValue());
            actionEntity.setExecutionOrder(action.executionOrder());
            entity.getActions().add(actionEntity);
        }
    }
}
