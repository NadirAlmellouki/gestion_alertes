package FST.MST_RSI.PFA.rulesengine.domain.service;

import FST.MST_RSI.PFA.classification.domain.model.ClassificationCategory;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationStatus;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRuleContext;
import FST.MST_RSI.PFA.rulesengine.domain.model.ConditionOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionEvaluatorTest {

    private ConditionEvaluator evaluator;
    private BusinessRuleContext context;

    @BeforeEach
    void setUp() {
        evaluator = new ConditionEvaluator();
        context = sampleContext(0.85, ClassificationStatus.SUCCESS);
    }

    @Test
    void evaluatesEqualsAndLessThan() {
        assertThat(evaluator.evaluate(context, "llm.confidence", ConditionOperator.LESS_THAN, "0.70")).isFalse();
        assertThat(evaluator.evaluate(sampleContext(0.55, ClassificationStatus.SUCCESS),
                "llm.confidence", ConditionOperator.LESS_THAN, "0.70")).isTrue();
        assertThat(evaluator.evaluate(context, "llm.status", ConditionOperator.EQUALS, "SUCCESS")).isTrue();
    }

    @Test
    void evaluatesContainsAndIn() {
        assertThat(evaluator.evaluate(context, "llm.matchedSolution", ConditionOperator.CONTAINS, "Pay")).isTrue();
        assertThat(evaluator.evaluate(context, "llm.matchedSolution", ConditionOperator.IN, "PayCore,Other")).isTrue();
    }

    private static BusinessRuleContext sampleContext(double confidence, ClassificationStatus status) {
        return new BusinessRuleContext(
                UUID.randomUUID(),
                "P-123",
                "CPU saturation",
                "CRITICAL",
                "APPLICATION",
                "OPEN",
                "DYNATRACE",
                Instant.now(),
                ClassificationCategory.RESOURCE_CONTENTION,
                "CPU_SATURATION",
                confidence,
                "PayCore",
                "Paiements",
                "Pilotage",
                "Core",
                status,
                false,
                "PSI-001",
                UUID.randomUUID(),
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
