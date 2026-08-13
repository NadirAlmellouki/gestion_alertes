package FST.MST_RSI.PFA.rulesengine.application.mapper;

import FST.MST_RSI.PFA.rulesengine.application.dto.RuleDto;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRule;
import FST.MST_RSI.PFA.rulesengine.domain.model.ConditionOperator;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleAction;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleCondition;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleConditionGroup;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleOrigin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class RuleDtoMapper {

    public RuleDto toDto(BusinessRule rule) {
        return new RuleDto(
                rule.id(),
                rule.code(),
                rule.name(),
                rule.description(),
                rule.evaluationPriority(),
                rule.enabled(),
                rule.stopOnMatch(),
                rule.origin(),
                rule.conditionGroups().stream().map(this::toGroupDto).toList(),
                rule.actions().stream().map(this::toActionDto).toList()
        );
    }

    public BusinessRule toDomain(RuleDto dto) {
        return new BusinessRule(
                dto.id() != null ? dto.id() : UUID.randomUUID(),
                dto.code(),
                dto.name(),
                dto.description(),
                dto.evaluationPriority(),
                dto.enabled(),
                dto.stopOnMatch(),
                dto.origin() != null ? dto.origin() : RuleOrigin.CONFIGURED,
                dto.conditionGroups().stream().map(this::toGroupDomain).toList(),
                dto.actions().stream().map(this::toActionDomain).toList()
        );
    }

    private RuleDto.RuleConditionGroupDto toGroupDto(RuleConditionGroup group) {
        return new RuleDto.RuleConditionGroupDto(
                group.id(),
                group.blockType(),
                group.logicalOperator(),
                group.executionOrder(),
                group.conditions().stream().map(this::toConditionDto).toList()
        );
    }

    private RuleDto.RuleConditionDto toConditionDto(RuleCondition condition) {
        return new RuleDto.RuleConditionDto(
                condition.id(),
                condition.fieldName(),
                condition.operator().name(),
                condition.expectedValue(),
                condition.valueType(),
                condition.conditionOrder()
        );
    }

    private RuleDto.RuleActionDto toActionDto(RuleAction action) {
        return new RuleDto.RuleActionDto(
                action.id(),
                action.actionType(),
                action.actionValue(),
                action.executionOrder()
        );
    }

    private RuleConditionGroup toGroupDomain(RuleDto.RuleConditionGroupDto dto) {
        List<RuleCondition> conditions = dto.conditions().stream().map(this::toConditionDomain).toList();
        return new RuleConditionGroup(
                dto.id(),
                dto.blockType(),
                dto.logicalOperator(),
                dto.executionOrder(),
                conditions
        );
    }

    private RuleCondition toConditionDomain(RuleDto.RuleConditionDto dto) {
        return new RuleCondition(
                dto.id(),
                dto.fieldName(),
                ConditionOperator.fromDb(dto.operator()),
                dto.expectedValue(),
                dto.valueType() != null ? dto.valueType() : "STRING",
                dto.conditionOrder()
        );
    }

    private RuleAction toActionDomain(RuleDto.RuleActionDto dto) {
        return new RuleAction(dto.id(), dto.actionType(), dto.actionValue(), dto.executionOrder());
    }
}
