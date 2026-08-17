package FST.MST_RSI.PFA.monitoring.application.usecase;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.monitoring.domain.model.ResolutionCheckStatus;
import FST.MST_RSI.PFA.monitoring.infrastructure.config.ResolutionCheckProperties;
import FST.MST_RSI.PFA.monitoring.infrastructure.persistence.ResolutionCheckEntity;
import FST.MST_RSI.PFA.monitoring.infrastructure.persistence.ResolutionCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ScheduleResolutionCheckUseCase {

    private static final Logger log = LoggerFactory.getLogger(ScheduleResolutionCheckUseCase.class);

    private final ResolutionCheckRepository resolutionCheckRepository;
    private final ResolutionCheckProperties properties;

    public ScheduleResolutionCheckUseCase(
            ResolutionCheckRepository resolutionCheckRepository,
            ResolutionCheckProperties properties
    ) {
        this.resolutionCheckRepository = resolutionCheckRepository;
        this.properties = properties;
    }

    @Transactional
    public void execute(Alert alert) {
        if (!properties.isEnabled()) {
            return;
        }
        if (alert.getExternalProblemId() == null || alert.getExternalProblemId().isBlank()) {
            return;
        }

        UUID alertId = alert.getId().value();
        if (resolutionCheckRepository.findFirstByAlertIdAndStatus(alertId, ResolutionCheckStatus.ACTIVE).isPresent()) {
            return;
        }

        Instant now = Instant.now();
        Instant firstCheckAt = now.plusSeconds(Math.max(0, properties.getInitialDelaySeconds()));
        ResolutionCheckEntity check = ResolutionCheckEntity.create(
                UUID.randomUUID(),
                alertId,
                alert.getExternalProblemId(),
                ResolutionCheckStatus.ACTIVE,
                firstCheckAt,
                now
        );
        resolutionCheckRepository.save(check);
        log.info("Scheduled Dynatrace resolution check for alert {} (problemId={}, firstCheckAt={})",
                alertId, alert.getExternalProblemId(), firstCheckAt);
    }
}
