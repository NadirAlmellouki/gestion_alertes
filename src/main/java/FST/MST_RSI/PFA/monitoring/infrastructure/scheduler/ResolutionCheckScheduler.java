package FST.MST_RSI.PFA.monitoring.infrastructure.scheduler;

import FST.MST_RSI.PFA.monitoring.application.usecase.ProcessResolutionCheckUseCase;
import FST.MST_RSI.PFA.monitoring.infrastructure.config.ResolutionCheckProperties;
import FST.MST_RSI.PFA.monitoring.infrastructure.persistence.ResolutionCheckEntity;
import FST.MST_RSI.PFA.monitoring.infrastructure.persistence.ResolutionCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ResolutionCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(ResolutionCheckScheduler.class);

    private final ResolutionCheckProperties properties;
    private final ResolutionCheckRepository resolutionCheckRepository;
    private final ProcessResolutionCheckUseCase processResolutionCheckUseCase;

    public ResolutionCheckScheduler(
            ResolutionCheckProperties properties,
            ResolutionCheckRepository resolutionCheckRepository,
            ProcessResolutionCheckUseCase processResolutionCheckUseCase
    ) {
        this.properties = properties;
        this.resolutionCheckRepository = resolutionCheckRepository;
        this.processResolutionCheckUseCase = processResolutionCheckUseCase;
    }

    @Scheduled(fixedDelayString = "${app.monitoring.resolution-check.poll-interval-ms:60000}")
    public void processDueChecks() {
        if (!properties.isEnabled()) {
            return;
        }
        List<ResolutionCheckEntity> due = resolutionCheckRepository.findDueChecks(Instant.now());
        if (due.isEmpty()) {
            return;
        }
        log.debug("Processing {} due Dynatrace resolution checks", due.size());
        for (ResolutionCheckEntity check : due) {
            try {
                processResolutionCheckUseCase.execute(check.getId());
            } catch (Exception ex) {
                log.error("Failed to process resolution check {}", check.getId(), ex);
            }
        }
    }
}
