package FST.MST_RSI.PFA.classification.application.usecase;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.classification.application.service.AlertContextExtractor;
import FST.MST_RSI.PFA.classification.domain.event.AlertClassifiedEvent;
import FST.MST_RSI.PFA.classification.domain.model.AlertClassificationContext;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationStatus;
import FST.MST_RSI.PFA.classification.domain.model.SolutionContext;
import FST.MST_RSI.PFA.classification.domain.port.AlertClassifierPort;
import FST.MST_RSI.PFA.classification.domain.port.BusinessContextPort;
import FST.MST_RSI.PFA.classification.domain.service.ClassificationPromptBuilder;
import FST.MST_RSI.PFA.classification.infrastructure.persistence.AlertLlmAnalysisEntity;
import FST.MST_RSI.PFA.classification.infrastructure.persistence.AlertLlmAnalysisPersistenceAdapter;
import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClassifyAlertUseCase {

    private static final Logger log = LoggerFactory.getLogger(ClassifyAlertUseCase.class);

    private final AlertRepositoryPort alertRepositoryPort;
    private final AlertContextExtractor alertContextExtractor;
    private final BusinessContextPort businessContextPort;
    private final AlertClassifierPort alertClassifierPort;
    private final AlertLlmAnalysisPersistenceAdapter llmAnalysisPersistenceAdapter;
    private final ApplicationEventPublisher eventPublisher;
    private final String promptVersion;
    private final String provider;

    public ClassifyAlertUseCase(
            AlertRepositoryPort alertRepositoryPort,
            AlertContextExtractor alertContextExtractor,
            BusinessContextPort businessContextPort,
            AlertClassifierPort alertClassifierPort,
            AlertLlmAnalysisPersistenceAdapter llmAnalysisPersistenceAdapter,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.classification.prompt-version:v2}") String promptVersion,
            @Value("${app.llm.provider:gemini}") String provider
    ) {
        this.alertRepositoryPort = alertRepositoryPort;
        this.alertContextExtractor = alertContextExtractor;
        this.businessContextPort = businessContextPort;
        this.alertClassifierPort = alertClassifierPort;
        this.llmAnalysisPersistenceAdapter = llmAnalysisPersistenceAdapter;
        this.eventPublisher = eventPublisher;
        this.promptVersion = promptVersion;
        this.provider = provider;
    }

    /** Wraps the classification result together with the persisted analysis entity. */
    public record ClassificationBundle(
            ClassificationResult result,
            AlertLlmAnalysisEntity analysis
    ) {}

    @Transactional
    public ClassificationBundle execute(String alertId) {
        Alert alert = alertRepositoryPort.findById(AlertId.of(alertId))
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));

        long started = System.currentTimeMillis();
        AlertClassificationContext alertContext = alertContextExtractor.extract(alert);
        List<SolutionContext> candidates = businessContextPort.findCandidates(
                alertContextExtractor.searchTerms(alert, alertContext)
        );

        if (candidates.isEmpty()) {
            log.warn("No business-context candidates for alert {}", alertId);
        }

        ClassificationResult result = alertClassifierPort.classify(alertContext, candidates);
        result = enrichWithOfficialPsi(result);
        long duration = System.currentTimeMillis() - started;

        String effectivePromptVersion = promptVersion.isBlank()
                ? ClassificationPromptBuilder.PROMPT_VERSION
                : promptVersion;

        AlertLlmAnalysisEntity savedAnalysis = llmAnalysisPersistenceAdapter.save(
                alert.getId().value(),
                result,
                provider,
                effectivePromptVersion,
                duration
        );

        eventPublisher.publishEvent(new AlertClassifiedEvent(
                alert.getId(),
                result.category(),
                result.confidence(),
                result.matchedSolution(),
                result.resolvedPsi(),
                result.status(),
                effectivePromptVersion,
                provider,
                duration
        ));

        log.info("Alert {} classified: status={}, category={}, confidence={}, psi={}, durationMs={}",
                alertId, result.status(), result.category(), result.confidence().value(),
                result.resolvedPsi(), duration);

        return new ClassificationBundle(result, savedAnalysis);
    }

    private ClassificationResult enrichWithOfficialPsi(ClassificationResult result) {
        if (result.status() == ClassificationStatus.FALLBACK || result.matchedSolution() == null) {
            return result;
        }
        return businessContextPort.findPsiBySolutionName(result.matchedSolution())
                .map(result::withResolvedPsi)
                .orElse(result);
    }
}
