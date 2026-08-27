package FST.MST_RSI.PFA.pipeline.api.rest;

import FST.MST_RSI.PFA.pipeline.application.ProcessAlertPipelineUseCase;
import FST.MST_RSI.PFA.pipeline.application.dto.PipelineResultDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertPipelineController {

    private final ProcessAlertPipelineUseCase processAlertPipelineUseCase;

    public AlertPipelineController(ProcessAlertPipelineUseCase processAlertPipelineUseCase) {
        this.processAlertPipelineUseCase = processAlertPipelineUseCase;
    }

    @PostMapping("/{alertId}/process")
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public PipelineResultDto process(@PathVariable String alertId) {
        ProcessAlertPipelineUseCase.PipelineResult result = processAlertPipelineUseCase.execute(alertId);
        return toDto(result);
    }

    private static PipelineResultDto toDto(ProcessAlertPipelineUseCase.PipelineResult result) {
        var classification = result.classification();
        var decision = result.businessDecision();
        var routing = result.routingDecision();
        var notification = result.notificationResult();
        Double confidence = classification == null || classification.confidence() == null
                ? null
                : classification.confidence().value();
        String channel = routing == null || routing.currentStep() == null ? null : routing.currentStep().channel();
        return new PipelineResultDto(
                result.alertId(),
                classification == null || classification.status() == null ? null : classification.status().name(),
                classification == null || classification.category() == null ? null : classification.category().name(),
                confidence,
                decision != null && decision.humanValidationRequired(),
                decision == null ? null : decision.matchedRuleCode(),
                decision != null && decision.routingTriggered(),
                routing == null ? null : routing.routingStatus(),
                routing == null ? null : routing.policyCode(),
                routing == null ? null : routing.selectedPersonName(),
                channel,
                notification == null ? null : notification.outcome(),
                notification == null ? null : notification.detail()
        );
    }
}
