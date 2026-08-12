package FST.MST_RSI.PFA.classification.domain.service;

import FST.MST_RSI.PFA.classification.domain.model.ClassificationCategory;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationStatus;
import FST.MST_RSI.PFA.classification.domain.model.SolutionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClassificationResponseValidatorTest {

    private ClassificationResponseValidator validator;
    private List<SolutionContext> candidates;

    @BeforeEach
    void setUp() {
        validator = new ClassificationResponseValidator(new ObjectMapper());
        candidates = List.of(new SolutionContext(
                "PayCore",
                "Core Processing & Services",
                "Pilotage",
                "Paiements",
                "InHouse",
                "P1",
                "Applicatif",
                "Banque",
                "Paiements"
        ));
    }

    @Test
    void parsesValidResponse() {
        String json = """
                {
                  "category": "RESOURCE_CONTENTION",
                  "problemType": "CPU_SATURATION",
                  "confidence": 0.91,
                  "matchedSolution": "PayCore",
                  "matchedDomaine": "Paiements",
                  "matchedPole": "Pilotage",
                  "matchedEntity": "Core Processing & Services",
                  "summary": "CPU saturé sur PayCore",
                  "probableCause": "Charge anormale",
                  "justification": "severity + entité PayCore",
                  "uncertainFields": [],
                  "requiresHumanValidation": false,
                  "fallback": false
                }
                """;

        var result = validator.parseAndValidate(json, candidates);

        assertThat(result.status()).isEqualTo(ClassificationStatus.SUCCESS);
        assertThat(result.category()).isEqualTo(ClassificationCategory.RESOURCE_CONTENTION);
        assertThat(result.matchedSolution()).isEqualTo("PayCore");
        assertThat(result.confidence().value()).isEqualTo(0.91);
        assertThat(result.resolvedPsi()).isNull();
    }

    @Test
    void rejectsUnknownSolution() {
        String json = """
                {
                  "category": "UNKNOWN",
                  "problemType": "X",
                  "confidence": 0.9,
                  "matchedSolution": "SolutionInventee",
                  "summary": "test",
                  "justification": "test",
                  "uncertainFields": [],
                  "requiresHumanValidation": false,
                  "fallback": false
                }
                """;

        var result = validator.parseAndValidate(json, candidates);

        assertThat(result.status()).isEqualTo(ClassificationStatus.FALLBACK);
    }

    @Test
    void rejectsPriorityFieldFromLlm() {
        String json = """
                {
                  "category": "UNKNOWN",
                  "problemType": "X",
                  "proposedPriority": "P1",
                  "confidence": 0.9,
                  "summary": "test",
                  "justification": "test"
                }
                """;

        var result = validator.parseAndValidate(json, candidates);

        assertThat(result.status()).isEqualTo(ClassificationStatus.FALLBACK);
    }

    @Test
    void invalidJsonReturnsFallback() {
        var result = validator.parseAndValidate("not-json", candidates);

        assertThat(result.status()).isEqualTo(ClassificationStatus.FALLBACK);
        assertThat(result.confidence().value()).isEqualTo(0.0);
        assertThat(result.requiresHumanValidation()).isTrue();
    }

    @Test
    void lowConfidenceMarkedAsLowConfidence() {
        String json = """
                {
                  "category": "UNKNOWN",
                  "problemType": "UNCLEAR",
                  "confidence": 0.3,
                  "matchedSolution": null,
                  "summary": "Peu d'indices",
                  "justification": "Contexte insuffisant",
                  "uncertainFields": ["matchedSolution"],
                  "requiresHumanValidation": true,
                  "fallback": true
                }
                """;

        var result = validator.parseAndValidate(json, candidates);

        assertThat(result.status()).isEqualTo(ClassificationStatus.FALLBACK);
        assertThat(result.requiresHumanValidation()).isTrue();
    }
}
