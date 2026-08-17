package FST.MST_RSI.PFA.routingengine.infrastructure.scheduler;

import FST.MST_RSI.PFA.routingengine.application.usecase.ProcessRoutingEscalationUseCase;
import FST.MST_RSI.PFA.routingengine.infrastructure.config.RoutingEscalationProperties;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionEntity;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class RoutingEscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoutingEscalationScheduler.class);

    private final RoutingEscalationProperties properties;
    private final RoutingExecutionRepository routingExecutionRepository;
    private final ProcessRoutingEscalationUseCase processRoutingEscalationUseCase;

    public RoutingEscalationScheduler(
            RoutingEscalationProperties properties,
            RoutingExecutionRepository routingExecutionRepository,
            ProcessRoutingEscalationUseCase processRoutingEscalationUseCase
    ) {
        this.properties = properties;
        this.routingExecutionRepository = routingExecutionRepository;
        this.processRoutingEscalationUseCase = processRoutingEscalationUseCase;
    }

    @Scheduled(fixedDelayString = "${app.routing.escalation.poll-interval-ms:30000}")
    public void processDueEscalations() {
        if (!properties.isEnabled()) {
            return;
        }
        List<RoutingExecutionEntity> due = routingExecutionRepository.findDueEscalations(Instant.now());
        if (due.isEmpty()) {
            return;
        }
        log.debug("Processing {} due routing escalations", due.size());
        for (RoutingExecutionEntity execution : due) {
            try {
                processRoutingEscalationUseCase.execute(execution.getId());
            } catch (Exception ex) {
                log.error("Failed to process escalation for execution {}", execution.getId(), ex);
            }
        }
    }
}
