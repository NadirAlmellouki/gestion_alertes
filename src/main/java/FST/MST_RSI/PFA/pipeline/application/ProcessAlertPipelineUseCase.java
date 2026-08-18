package FST.MST_RSI.PFA.pipeline.application;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.audit.application.service.AuditRecorder;
import FST.MST_RSI.PFA.audit.domain.model.AuditAction;
import FST.MST_RSI.PFA.audit.domain.model.AuditRecord;
import FST.MST_RSI.PFA.classification.application.usecase.ClassifyAlertUseCase;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.classification.infrastructure.persistence.AlertLlmAnalysisEntity;
import FST.MST_RSI.PFA.classification.infrastructure.persistence.AlertLlmAnalysisRepository;
import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import FST.MST_RSI.PFA.notification.application.usecase.ExecuteNotificationWorkflowUseCase;
import FST.MST_RSI.PFA.notification.domain.model.NotificationStatus;
import FST.MST_RSI.PFA.routingengine.application.usecase.ScheduleRoutingEscalationUseCase;
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

import java.util.List;
import java.util.UUID;

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
    private final ScheduleRoutingEscalationUseCase scheduleRoutingEscalationUseCase;
    private final AuditRecorder auditRecorder;

    public ProcessAlertPipelineUseCase(
            AlertRepositoryPort alertRepositoryPort,
            ClassifyAlertUseCase classifyAlertUseCase,
            AlertLlmAnalysisRepository llmAnalysisRepository,
            BusinessRuleContextBuilder businessRuleContextBuilder,
            BusinessRuleEngine businessRuleEngine,
            PersonResolver personResolver,
            RoutingEngine routingEngine,
            ExecuteNotificationWorkflowUseCase executeNotificationWorkflowUseCase,
            ScheduleRoutingEscalationUseCase scheduleRoutingEscalationUseCase,
            AuditRecorder auditRecorder
    ) {
        this.alertRepositoryPort = alertRepositoryPort;
        this.classifyAlertUseCase = classifyAlertUseCase;
        this.llmAnalysisRepository = llmAnalysisRepository;
        this.businessRuleContextBuilder = businessRuleContextBuilder;
        this.businessRuleEngine = businessRuleEngine;
        this.personResolver = personResolver;
        this.routingEngine = routingEngine;
        this.executeNotificationWorkflowUseCase = executeNotificationWorkflowUseCase;
        this.scheduleRoutingEscalationUseCase = scheduleRoutingEscalationUseCase;
        this.auditRecorder = auditRecorder;
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
        auditRuleDecision(alert.getId().value(), analysis.getId(), businessDecision);

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
        auditRoutingDecision(alert.getId().value(), analysis.getId(), routingDecision);

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

        scheduleEscalationIfNeeded(routingDecision, notificationResult);

        auditRecorder.record(new AuditRecord(
                AuditAction.PIPELINE_COMPLETED,
                alert.getId().value(),
                analysis.getId(),
                routingDecision.routingExecutionId(),
                notificationResult.notificationId(),
                null,
                "AlertPipeline",
                alert.getId().value(),
                "Pipeline terminé: rule=" + businessDecision.matchedRuleCode()
                        + ", routingStatus=" + routingDecision.routingStatus()
                        + ", notificationOutcome=" + notificationResult.outcome(),
                AuditRecorder.correlationId(alert.getId().value()),
                null,
                List.of()
        ));

        return new PipelineResult(alertId, classification, businessDecision, routingDecision, notificationResult);
    }

    private void auditRuleDecision(UUID alertId, UUID analysisId, BusinessDecision decision) {
        auditRecorder.record(new AuditRecord(
                AuditAction.RULE_EVALUATED,
                alertId,
                analysisId,
                null,
                null,
                null,
                "BusinessRule",
                decision.matchedRuleId(),
                "Règle appliquée: " + decision.matchedRuleCode()
                        + ", origin=" + decision.matchedRuleOrigin()
                        + ", routingTriggered=" + decision.routingTriggered()
                        + ", humanValidation=" + decision.humanValidationRequired(),
                AuditRecorder.correlationId(alertId),
                null,
                List.of(
                        new AuditRecord.AuditDetail("forcedRole", null, decision.forcedRole()),
                        new AuditRecord.AuditDetail("selectedSolution", null, decision.selectedSolutionName())
                )
        ));
    }

    private void auditRoutingDecision(UUID alertId, UUID analysisId, RoutingDecision decision) {
        String person = decision.selectedPersonName() != null ? decision.selectedPersonName() : "none";
        String channel = decision.currentStep() != null ? decision.currentStep().channel() : "none";
        auditRecorder.record(new AuditRecord(
                AuditAction.ROUTING_DECIDED,
                alertId,
                analysisId,
                decision.routingExecutionId(),
                null,
                decision.selectedPersonId(),
                "RoutingPolicy",
                decision.policyId(),
                "Routage: policy=" + decision.policyCode()
                        + ", status=" + decision.routingStatus()
                        + ", person=" + person
                        + ", channel=" + channel,
                AuditRecorder.correlationId(alertId),
                null,
                List.of()
        ));
    }

    private void scheduleEscalationIfNeeded(
            RoutingDecision routingDecision,
            ExecuteNotificationWorkflowUseCase.NotificationWorkflowResult notificationResult
    ) {
        if (routingDecision.routingExecutionId() == null || routingDecision.currentStep() == null) {
            return;
        }
        if (!"STARTED".equals(routingDecision.routingStatus())) {
            return;
        }
        if (notificationResult.status() == NotificationStatus.SKIPPED) {
            return;
        }
        scheduleRoutingEscalationUseCase.execute(
                routingDecision.routingExecutionId(),
                routingDecision.currentStep().stepOrder()
        );
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
