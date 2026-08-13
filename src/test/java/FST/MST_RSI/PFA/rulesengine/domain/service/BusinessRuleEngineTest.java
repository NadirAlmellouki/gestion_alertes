package FST.MST_RSI.PFA.rulesengine.domain.service;

import FST.MST_RSI.PFA.classification.domain.model.ClassificationCategory;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationStatus;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessDecision;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRule;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRuleContext;
import FST.MST_RSI.PFA.rulesengine.domain.model.ConditionBlockType;
import FST.MST_RSI.PFA.rulesengine.domain.model.ConditionOperator;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleAction;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleCondition;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleConditionGroup;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleOrigin;
import FST.MST_RSI.PFA.rulesengine.domain.port.RuleRepositoryPort;
import FST.MST_RSI.PFA.rulesengine.infrastructure.persistence.RuleExecutionEntity;
import FST.MST_RSI.PFA.rulesengine.infrastructure.persistence.RuleExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessRuleEngineTest {

    @Mock
    private RuleRepositoryPort ruleRepositoryPort;

    @Mock
    private RuleExecutionRepository ruleExecutionRepository;

    private BusinessRuleEngine engine;
    private UUID analysisId;

    @BeforeEach
    void setUp() {
        engine = new BusinessRuleEngine(ruleRepositoryPort, new ConditionEvaluator(), ruleExecutionRepository);
        analysisId = UUID.randomUUID();
        when(ruleExecutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void lowConfidenceTriggersHumanValidation() {
        UUID ruleId = UUID.randomUUID();
        when(ruleRepositoryPort.findEnabledByOriginOrderByPriority(RuleOrigin.CONFIGURED)).thenReturn(List.of());
        when(ruleRepositoryPort.findEnabledByOriginOrderByPriority(RuleOrigin.DEFAULT)).thenReturn(List.of(
                lowConfidenceRule(ruleId)
        ));

        BusinessDecision decision = engine.evaluate(contextWithConfidence(0.55), analysisId);

        assertThat(decision.humanValidationRequired()).isTrue();
        assertThat(decision.routingTriggered()).isFalse();
        assertThat(decision.matchedRuleCode()).isEqualTo("DEFAULT-LOW-CONFIDENCE");

        ArgumentCaptor<RuleExecutionEntity> captor = ArgumentCaptor.forClass(RuleExecutionEntity.class);
        verify(ruleExecutionRepository).save(captor.capture());
        assertThat(captor.getValue().isMatched()).isTrue();
    }

    @Test
    void highConfidenceTriggersDefaultRouting() {
        when(ruleRepositoryPort.findEnabledByOriginOrderByPriority(RuleOrigin.CONFIGURED)).thenReturn(List.of());
        when(ruleRepositoryPort.findEnabledByOriginOrderByPriority(RuleOrigin.DEFAULT)).thenReturn(List.of(
                lowConfidenceRule(UUID.randomUUID()),
                standardRoutingRule(UUID.randomUUID())
        ));

        BusinessDecision decision = engine.evaluate(contextWithConfidence(0.92), analysisId);

        assertThat(decision.humanValidationRequired()).isFalse();
        assertThat(decision.routingTriggered()).isTrue();
        assertThat(decision.matchedRuleCode()).isEqualTo("DEFAULT-STANDARD-ROUTING");
    }

    @Test
    void configuredRuleTakesPrecedenceOverDefault() {
        UUID configuredId = UUID.randomUUID();
        when(ruleRepositoryPort.findEnabledByOriginOrderByPriority(RuleOrigin.CONFIGURED)).thenReturn(List.of(
                forcedRoleRule(configuredId)
        ));

        BusinessDecision decision = engine.evaluate(contextWithConfidence(0.92), analysisId);

        assertThat(decision.forcedRole()).isEqualTo("MANAGER");
        assertThat(decision.matchedRuleOrigin()).isEqualTo(RuleOrigin.CONFIGURED);
    }

    @Test
    void fallbackStatusRequestsHumanValidation() {
        when(ruleRepositoryPort.findEnabledByOriginOrderByPriority(RuleOrigin.CONFIGURED)).thenReturn(List.of());
        when(ruleRepositoryPort.findEnabledByOriginOrderByPriority(RuleOrigin.DEFAULT)).thenReturn(List.of(
                fallbackRule(UUID.randomUUID()),
                standardRoutingRule(UUID.randomUUID())
        ));

        BusinessDecision decision = engine.evaluate(
                contextWithConfidenceAndStatus(0.0, ClassificationStatus.FALLBACK),
                analysisId
        );

        assertThat(decision.humanValidationRequired()).isTrue();
        assertThat(decision.matchedRuleCode()).isEqualTo("DEFAULT-LLM-FALLBACK");
    }

    private BusinessRule lowConfidenceRule(UUID id) {
        return new BusinessRule(
                id,
                "DEFAULT-LOW-CONFIDENCE",
                "Low confidence",
                null,
                10,
                true,
                true,
                RuleOrigin.DEFAULT,
                List.of(new RuleConditionGroup(
                        UUID.randomUUID(),
                        ConditionBlockType.CONDITION,
                        "AND",
                        0,
                        List.of(new RuleCondition(
                                UUID.randomUUID(),
                                "llm.confidence",
                                ConditionOperator.LESS_THAN,
                                "0.70",
                                "NUMBER",
                                0
                        ))
                )),
                List.of(new RuleAction(UUID.randomUUID(), "REQUEST_HUMAN_VALIDATION", null, 0))
        );
    }

    private BusinessRule fallbackRule(UUID id) {
        return new BusinessRule(
                id,
                "DEFAULT-LLM-FALLBACK",
                "Fallback",
                null,
                20,
                true,
                true,
                RuleOrigin.DEFAULT,
                List.of(new RuleConditionGroup(
                        UUID.randomUUID(),
                        ConditionBlockType.CONDITION,
                        "AND",
                        0,
                        List.of(new RuleCondition(
                                UUID.randomUUID(),
                                "llm.status",
                                ConditionOperator.EQUALS,
                                "FALLBACK",
                                "STRING",
                                0
                        ))
                )),
                List.of(new RuleAction(UUID.randomUUID(), "REQUEST_HUMAN_VALIDATION", null, 0))
        );
    }

    private BusinessRule standardRoutingRule(UUID id) {
        return new BusinessRule(
                id,
                "DEFAULT-STANDARD-ROUTING",
                "Standard routing",
                null,
                1000,
                true,
                true,
                RuleOrigin.DEFAULT,
                List.of(new RuleConditionGroup(
                        UUID.randomUUID(),
                        ConditionBlockType.CONDITION,
                        "AND",
                        0,
                        List.of()
                )),
                List.of(
                        new RuleAction(UUID.randomUUID(), "SELECT_BUSINESS_CONTEXT", null, 0),
                        new RuleAction(UUID.randomUUID(), "TRIGGER_ROUTING", null, 1)
                )
        );
    }

    private BusinessRule forcedRoleRule(UUID id) {
        return new BusinessRule(
                id,
                "CFG-FORCE-MANAGER",
                "Force manager",
                null,
                1,
                true,
                true,
                RuleOrigin.CONFIGURED,
                List.of(new RuleConditionGroup(
                        UUID.randomUUID(),
                        ConditionBlockType.CONDITION,
                        "AND",
                        0,
                        List.of(new RuleCondition(
                                UUID.randomUUID(),
                                "llm.matchedSolution",
                                ConditionOperator.EQUALS,
                                "PayCore",
                                "STRING",
                                0
                        ))
                )),
                List.of(
                        new RuleAction(UUID.randomUUID(), "SET_FORCED_ROLE", "MANAGER", 0),
                        new RuleAction(UUID.randomUUID(), "TRIGGER_ROUTING", null, 1)
                )
        );
    }

    private BusinessRuleContext contextWithConfidence(double confidence) {
        return contextWithConfidenceAndStatus(confidence, ClassificationStatus.SUCCESS);
    }

    private BusinessRuleContext contextWithConfidenceAndStatus(double confidence, ClassificationStatus status) {
        UUID solutionId = UUID.randomUUID();
        return new BusinessRuleContext(
                UUID.randomUUID(),
                "P-1",
                "Alert",
                "CRITICAL",
                "APPLICATION",
                "OPEN",
                "DYNATRACE",
                Instant.now(),
                ClassificationCategory.RESOURCE_CONTENTION,
                "CPU",
                confidence,
                "PayCore",
                "Paiements",
                "Pilotage",
                "Core",
                status,
                false,
                "PSI-1",
                solutionId,
                "PayCore",
                "Paiements",
                "Pilotage",
                "Core",
                "BUSINESS",
                "API",
                "PROD",
                "INTERNAL",
                true,
                false,
                List.of(),
                Map.of()
        );
    }
}
