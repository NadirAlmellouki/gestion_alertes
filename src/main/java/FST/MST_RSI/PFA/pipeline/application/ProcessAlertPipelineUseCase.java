package FST.MST_RSI.PFA.pipeline.application;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.classification.application.usecase.ClassifyAlertUseCase;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.classification.infrastructure.persistence.AlertLlmAnalysisEntity;
import FST.MST_RSI.PFA.classification.infrastructure.persistence.AlertLlmAnalysisRepository;
import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import FST.MST_RSI.PFA.notification.application.usecase.ExecuteNotificationWorkflowUseCase;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingContext;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingDecision;
import FST.MST_RSI.PFA.routingengine.domain.service.PersonResolver;
import FST.MST_RSI.PFA.routingengine.domain.service.RoutingEngine;
import FST.MST_RSI.PFA.rulesengine.application.service.BusinessRuleContextBuilder;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessDecision;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRuleContext;
import FST.MST_RSI.PFA.rulesengine.domain.service.BusinessRuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessAlertPipelineUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessAlertPipelineUseCase.class);

    private final AlertRepositoryPort alertRepositoryPort;
    private final ClassifyAlertUseCase classifyAlertUseCase;
    private final AlertLlmAnalysisRepository llmAnalysisRepository;
    private final BusinessRuleContextBuilder businessRuleContextBuilder;
    private final BusinessRuleEngine businessRuleEngine;
    private final PersonResolver personResolver;
    private final RoutingEngine routingEngine;
    private final ExecuteNotificationWorkflowUseCase executeNotificationWorkflowUseCase;

    public ProcessAlertPipelineUseCase(
            AlertRepositoryPort alertRepositoryPort,
            ClassifyAlertUseCase classifyAlertUseCase,
            AlertLlmAnalysisRepository llmAnalysisRepository,
            BusinessRuleContextBuilder businessRuleContextBuilder,
            BusinessRuleEngine businessRuleEngine,
            PersonResolver personResolver,
            RoutingEngine routingEngine,
            ExecuteNotificationWorkflowUseCase executeNotificationWorkflowUseCase
    ) {
        this.alertRepositoryPort = alertRepositoryPort;
        this.classifyAlertUseCase = classifyAlertUseCase;
        this.llmAnalysisRepository = llmAnalysisRepository;
        this.businessRuleContextBuilder = businessRuleContextBuilder;
        this.businessRuleEngine = businessRuleEngine;
        this.personResolver = personResolver;
        this.routingEngine = routingEngine;
        this.executeNotificationWorkflowUseCase = executeNotificationWorkflowUseCase;
    }

    @Transactional
    public PipelineResult execute(String alertId) {
        Alert alert = alertRepositoryPort.findById(AlertId.of(alertId))
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));

        ClassificationResult classification = classifyAlertUseCase.execute(alertId);
        AlertLlmAnalysisEntity analysis = llmAnalysisRepository.findTopByAlertIdOrderByCreatedAtDesc(alert.getId().value())
                .orElseThrow(() -> new IllegalStateException("LLM analysis missing for alert " + alertId));

        BusinessRuleContext ruleContext = businessRuleContextBuilder.build(alert, analysis);
        BusinessDecision businessDecision = businessRuleEngine.evaluate(ruleContext, analysis.getId());

        PersonResolver.HierarchyIds hierarchy = resolveHierarchy(ruleContext);
        RoutingContext routingContext = new RoutingContext(
                alert.getId().value(),
                analysis.getId(),
                businessDecision,
                hierarchy.solutionId(),
                ruleContext.resolvedSolutionName(),
                hierarchy.domainId(),
                hierarchy.poleId(),
                hierarchy.entityId(),
                businessDecision.forcedRole()
        );
        RoutingDecision routingDecision = routingEngine.buildRoutingDecision(routingContext);

        ExecuteNotificationWorkflowUseCase.NotificationWorkflowResult notificationResult =
                executeNotificationWorkflowUseCase.execute(
                        new ExecuteNotificationWorkflowUseCase.NotificationWorkflowCommand(
                                alert,
                                classification,
                                businessDecision,
                                routingDecision
                        )
                );

        log.info("Pipeline completed for alert {}: rule={}, routingStatus={}, notificationOutcome={}",
                alertId, businessDecision.matchedRuleCode(), routingDecision.routingStatus(),
                notificationResult.outcome());

        return new PipelineResult(alertId, classification, businessDecision, routingDecision, notificationResult);
    }

    private PersonResolver.HierarchyIds resolveHierarchy(BusinessRuleContext ruleContext) {
        if (ruleContext.resolvedSolutionUnitId() == null) {
            return new PersonResolver.HierarchyIds(null, null, null, null);
        }
        return personResolver.resolveHierarchy(ruleContext.resolvedSolutionUnitId());
    }

    public record PipelineResult(
            String alertId,
            ClassificationResult classification,
            BusinessDecision businessDecision,
            RoutingDecision routingDecision,
            ExecuteNotificationWorkflowUseCase.NotificationWorkflowResult notificationResult
    ) {
    }
}
