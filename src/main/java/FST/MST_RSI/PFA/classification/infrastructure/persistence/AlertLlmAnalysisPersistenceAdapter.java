package FST.MST_RSI.PFA.classification.infrastructure.persistence;

import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class AlertLlmAnalysisPersistenceAdapter {

    private final AlertLlmAnalysisRepository repository;
    private final ObjectMapper objectMapper;

    public AlertLlmAnalysisPersistenceAdapter(
            AlertLlmAnalysisRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void save(
            UUID alertId,
            ClassificationResult result,
            String provider,
            String promptVersion,
            long durationMs
    ) {
        repository.save(AlertLlmAnalysisEntity.create(
                UUID.randomUUID(),
                alertId,
                provider,
                promptVersion,
                (int) durationMs,
                result.status(),
                result.category(),
                result.problemType(),
                BigDecimal.valueOf(result.confidence().value()),
                result.matchedSolution(),
                result.matchedDomaine(),
                result.matchedPole(),
                result.matchedEntity(),
                result.resolvedPsi(),
                result.summary(),
                result.probableCause(),
                result.justification(),
                writeUncertainFields(result.uncertainFields()),
                result.requiresHumanValidation(),
                result.errorMessage(),
                Instant.now()
        ));
    }

    private String writeUncertainFields(java.util.List<String> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
