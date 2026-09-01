package FST.MST_RSI.PFA.pipeline;

import FST.MST_RSI.PFA.alerting.domain.event.AlertReceivedEvent;
import FST.MST_RSI.PFA.pipeline.application.ProcessAlertPipelineUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.Executor;

@Service
public class AlertProcessingOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AlertProcessingOrchestrator.class);

    private final ProcessAlertPipelineUseCase processAlertPipelineUseCase;
    private final Executor taskExecutor;

    public AlertProcessingOrchestrator(
            ProcessAlertPipelineUseCase processAlertPipelineUseCase,
            @Qualifier("taskExecutor") Executor taskExecutor
    ) {
        this.processAlertPipelineUseCase = processAlertPipelineUseCase;
        this.taskExecutor = taskExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAlertReceived(AlertReceivedEvent event) {
        taskExecutor.execute(() -> {
            try {
                processAlertPipelineUseCase.execute(event.getAlertId().value().toString());
            } catch (Exception ex) {
                log.error("Pipeline failed for alert {}", event.getAlertId().value(), ex);
            }
        });
    }
}
