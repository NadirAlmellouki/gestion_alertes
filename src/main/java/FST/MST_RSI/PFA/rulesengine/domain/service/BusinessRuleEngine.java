package FST.MST_RSI.PFA.rulesengine.domain.service;

import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessDecision;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRule;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRuleContext;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleAction;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleCondition;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleConditionGroup;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleOrigin;
import FST.MST_RSI.PFA.rulesengine.domain.port.RuleRepositoryPort;
import FST.MST_RSI.PFA.rulesengine.infrastructure.persistence.RuleExecutionEntity;
import FST.MST_RSI.PFA.rulesengine.infrastructure.persistence.RuleExecutionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BusinessRuleEngine {

    private final RuleRepositoryPort ruleRepositoryPort;
    private final ConditionEvaluator conditionEvaluator;
    private final RuleExecutionRepository ruleExecutionRepository;

    public BusinessRuleEngine(
            RuleRepositoryPort ruleRepositoryPort,
            ConditionEvaluator conditionEvaluator,
            RuleExecutionRepository ruleExecutionRepository
    ) {
        this.ruleRepositoryPort = ruleRepositoryPort;
        this.conditionEvaluator = conditionEvaluator;
        this.ruleExecutionRepository = ruleExecutionRepository;
    }

    public BusinessDecision evaluate(BusinessRuleContext context, UUID llmAnalysisId) {
        BusinessDecision configured = evaluateOrigin(context, RuleOrigin.CONFIGURED, llmAnalysisId);
        if (configured.matchedRuleId() != null) {
            return configured;
        }
        return evaluateOrigin(context, RuleOrigin.DEFAULT, llmAnalysisId);
    }

    private BusinessDecision evaluateOrigin(BusinessRuleContext context, RuleOrigin origin, UUID llmAnalysisId) {
        BusinessDecision aggregate = BusinessDecision.noMatch();
        for (BusinessRule rule : ruleRepositoryPort.findEnabledByOriginOrderByPriority(origin)) {
            long started = System.currentTimeMillis();
            boolean matched = matchesRule(context, rule);
            persistExecution(rule, context, llmAnalysisId, matched, started);
            if (matched) {
                BusinessDecision decision = applyActions(rule, context);
                aggregate = aggregate.merge(decision);
                if (rule.stopOnMatch()) {
                    return aggregate;
                }
            }
        }
        return aggregate;
    }

    private boolean matchesRule(BusinessRuleContext context, BusinessRule rule) {
        if (!evaluateGroups(context, rule.conditionBlocks())) {
            return false;
        }
        if (!rule.exceptionBlocks().isEmpty() && evaluateGroups(context, rule.exceptionBlocks())) {
            return false;
        }
        return true;
    }

    private boolean evaluateGroups(BusinessRuleContext context, List<RuleConditionGroup> groups) {
        if (groups.isEmpty()) {
            return true;
        }
        boolean result = evaluateGroup(context, groups.getFirst());
        for (int i = 1; i < groups.size(); i++) {
            RuleConditionGroup group = groups.get(i);
            boolean groupMatch = evaluateGroup(context, group);
            if ("OR".equalsIgnoreCase(group.logicalOperator())) {
                result = result || groupMatch;
            } else {
                result = result && groupMatch;
            }
        }
        return result;
    }

    private boolean evaluateGroup(BusinessRuleContext context, RuleConditionGroup group) {
        if (group.conditions().isEmpty()) {
            return true;
        }
        boolean result = conditionEvaluator.evaluate(
                context,
                group.conditions().getFirst().fieldName(),
                group.conditions().getFirst().operator(),
                group.conditions().getFirst().expectedValue()
        );
        for (int i = 1; i < group.conditions().size(); i++) {
            RuleCondition condition = group.conditions().get(i);
            boolean match = conditionEvaluator.evaluate(context, condition.fieldName(), condition.operator(), condition.expectedValue());
            if ("OR".equalsIgnoreCase(group.logicalOperator())) {
                result = result || match;
            } else {
                result = result && match;
            }
        }
        return result;
    }

    private BusinessDecision applyActions(BusinessRule rule, BusinessRuleContext context) {
        boolean humanValidation = false;
        boolean routing = false;
        String forcedRole = null;
        List<String> actions = new ArrayList<>();
        for (RuleAction action : rule.actions()) {
            actions.add(action.actionType());
            switch (action.actionType()) {
                case "REQUEST_HUMAN_VALIDATION" -> humanValidation = true;
                case "TRIGGER_ROUTING" -> routing = true;
                case "SELECT_BUSINESS_CONTEXT" -> { /* context already resolved upstream */ }
                case "SET_FORCED_ROLE" -> forcedRole = action.actionValue();
                default -> { /* extensible */ }
            }
        }
        return new BusinessDecision(
                rule.id(),
                rule.code(),
                rule.origin(),
                humanValidation,
                routing,
                context.resolvedSolutionUnitId(),
                context.resolvedSolutionName(),
                forcedRole,
                List.of(),
                null,
                actions
        );
    }

    private void persistExecution(
            BusinessRule rule,
            BusinessRuleContext context,
            UUID llmAnalysisId,
            boolean matched,
            long started
    ) {
        ruleExecutionRepository.save(RuleExecutionEntity.create(
                UUID.randomUUID(),
                rule.id(),
                context.alertId(),
                llmAnalysisId,
                matched,
                (int) (System.currentTimeMillis() - started),
                Instant.now()
        ));
    }
}
