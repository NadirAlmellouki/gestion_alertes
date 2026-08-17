package FST.MST_RSI.PFA.routingengine.application.service;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.classification.infrastructure.persistence.AlertLlmAnalysisEntity;
import FST.MST_RSI.PFA.classification.infrastructure.persistence.AlertLlmAnalysisRepository;
import FST.MST_RSI.PFA.common.domain.vo.Confidence;
import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingContext;
import FST.MST_RSI.PFA.routingengine.domain.service.PersonResolver;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RoutingEscalationContextLoader {

    private final AlertRepositoryPort alertRepositoryPort;
    private final AlertLlmAnalysisRepository llmAnalysisRepository;
    private final PersonResolver personResolver;

    public RoutingEscalationContextLoader(
            AlertRepositoryPort alertRepositoryPort,
            AlertLlmAnalysisRepository llmAnalysisRepository,
            PersonResolver personResolver
    ) {
        this.alertRepositoryPort = alertRepositoryPort;
        this.llmAnalysisRepository = llmAnalysisRepository;
        this.personResolver = personResolver;
    }

    public LoadedEscalationContext load(RoutingExecutionEntity execution) {
        Alert alert = alertRepositoryPort.findById(AlertId.of(execution.getAlertId().toString()))
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + execution.getAlertId()));

        AlertLlmAnalysisEntity analysis = llmAnalysisRepository.findById(execution.getClassificationId())
                .orElseGet(() -> llmAnalysisRepository.findTopByAlertIdOrderByCreatedAtDesc(execution.getAlertId())
                        .orElseThrow(() -> new IllegalStateException("LLM analysis missing for alert " + execution.getAlertId())));

        UUID solutionId = execution.getSelectedSolutionId();
        PersonResolver.HierarchyIds hierarchy = solutionId == null
                ? new PersonResolver.HierarchyIds(null, null, null, null)
                : personResolver.resolveHierarchy(solutionId);

        RoutingContext routingContext = new RoutingContext(
                execution.getAlertId(),
                analysis.getId(),
                null,
                hierarchy.solutionId(),
                analysis.getMatchedSolution(),
                hierarchy.domainId(),
                hierarchy.poleId(),
                hierarchy.entityId(),
                null
        );

        ClassificationResult classification = new ClassificationResult(
                analysis.getCategory(),
                analysis.getProblemType(),
                analysis.getConfidence() == null ? new Confidence(0) : new Confidence(analysis.getConfidence().doubleValue()),
                analysis.getMatchedSolution(),
                analysis.getMatchedDomain(),
                analysis.getMatchedPole(),
                analysis.getMatchedEntity(),
                analysis.getSummary(),
                analysis.getProbableCause(),
                analysis.getJustification(),
                List.of(),
                analysis.isRequiresHumanValidation(),
                analysis.getStatus(),
                null,
                null
        );

        return new LoadedEscalationContext(alert, classification, routingContext);
    }

    public record LoadedEscalationContext(
            Alert alert,
            ClassificationResult classification,
            RoutingContext routingContext
    ) {
    }
}
