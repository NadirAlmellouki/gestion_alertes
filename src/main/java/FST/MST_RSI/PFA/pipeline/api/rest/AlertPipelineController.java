package FST.MST_RSI.PFA.pipeline.api.rest;

import FST.MST_RSI.PFA.pipeline.application.ProcessAlertPipelineUseCase;
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
    public ProcessAlertPipelineUseCase.PipelineResult process(@PathVariable String alertId) {
        return processAlertPipelineUseCase.execute(alertId);
    }
}
