package FST.MST_RSI.PFA.monitoring.application.usecase;

import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.monitoring.domain.model.DynatraceProblemSnapshot;
import FST.MST_RSI.PFA.monitoring.domain.model.ResolutionCheckStatus;
import FST.MST_RSI.PFA.monitoring.domain.port.DynatraceProblemPort;
import FST.MST_RSI.PFA.monitoring.infrastructure.config.ResolutionCheckProperties;
import FST.MST_RSI.PFA.monitoring.infrastructure.persistence.ResolutionCheckEntity;
import FST.MST_RSI.PFA.monitoring.infrastructure.persistence.ResolutionCheckRepository;
import FST.MST_RSI.PFA.routingengine.domain.service.RoutingEscalationEngine;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionEntity;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProcessResolutionCheckUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessResolutionCheckUseCase.class);

    private final ResolutionCheckRepository resolutionCheckRepository;
    private final DynatraceProblemPort dynatraceProblemPort;
    private final AlertRepositoryPort alertRepositoryPort;
    private final RoutingExecutionRepository routingExecutionRepository;
    private final RoutingEscalationEngine routingEscalationEngine;
    private final ResolutionCheckProperties properties;

    public ProcessResolutionCheckUseCase(
            ResolutionCheckRepository resolutionCheckRepository,
            DynatraceProblemPort dynatraceProblemPort,
            AlertRepositoryPort alertRepositoryPort,
            RoutingExecutionRepository routingExecutionRepository,
            RoutingEscalationEngine routingEscalationEngine,
            ResolutionCheckProperties properties
    ) {
        this.resolutionCheckRepository = resolutionCheckRepository;
        this.dynatraceProblemPort = dynatraceProblemPort;
        this.alertRepositoryPort = alertRepositoryPort;
        this.routingExecutionRepository = routingExecutionRepository;
        this.routingEscalationEngine = routingEscalationEngine;
        this.properties = properties;
    }

    @Transactional
    public void execute(UUID resolutionCheckId) {
        ResolutionCheckEntity check = resolutionCheckRepository.findById(resolutionCheckId).orElse(null);
        if (check == null || !ResolutionCheckStatus.ACTIVE.equals(check.getStatus())) {
            return;
        }

        if (isExpired(check)) {
            finish(check, ResolutionCheckStatus.EXPIRED, null);
            log.info("Resolution check expired for alert {} after {} attempts",
                    check.getAlertId(), check.getAttemptCount());
            return;
        }

        if (check.getAttemptCount() >= properties.getMaxAttempts()) {
            finish(check, ResolutionCheckStatus.EXPIRED, null);
            log.info("Resolution check max attempts reached for alert {}", check.getAlertId());
            return;
        }

        check.setAttemptCount(check.getAttemptCount() + 1);
        Optional<DynatraceProblemSnapshot> snapshot = dynatraceProblemPort.fetchProblem(check.getExternalProblemId());
        if (snapshot.isEmpty()) {
            scheduleNextAttempt(check, "Dynatrace API unavailable");
            resolutionCheckRepository.save(check);
            return;
        }

        DynatraceProblemSnapshot problem = snapshot.get();
        check.setLastDynatraceState(problem.status());
        check.setLastError(null);

        if (problem.isResolved()) {
            markAlertResolved(check.getAlertId(), problem.status());
            stopActiveRouting(check.getAlertId());
            finish(check, ResolutionCheckStatus.RESOLVED, problem.status());
            log.info("Dynatrace problem resolved for alert {} (problemId={})",
                    check.getAlertId(), check.getExternalProblemId());
            return;
        }

        scheduleNextAttempt(check, null);
        resolutionCheckRepository.save(check);
        log.debug("Dynatrace problem still open for alert {} (attempt={})",
                check.getAlertId(), check.getAttemptCount());
    }

    private void markAlertResolved(UUID alertId, String dynatraceState) {
        alertRepositoryPort.findById(AlertId.of(alertId.toString()))
                .ifPresent(alert -> {
                    alert.updateDynatraceState(dynatraceState);
                    alertRepositoryPort.save(alert);
                });
    }

    private void stopActiveRouting(UUID alertId) {
        List<RoutingExecutionEntity> active = routingExecutionRepository.findActiveByAlertId(alertId);
        for (RoutingExecutionEntity execution : active) {
            routingEscalationEngine.complete(execution, "Dynatrace problem resolved");
        }
    }

    private void scheduleNextAttempt(ResolutionCheckEntity check, String error) {
        check.setLastError(error);
        check.setNextCheckAt(Instant.now().plusSeconds(Math.max(1, properties.getPollingIntervalSeconds())));
    }

    private void finish(ResolutionCheckEntity check, String status, String dynatraceState) {
        check.setStatus(status);
        check.setNextCheckAt(null);
        check.setFinishedAt(Instant.now());
        if (dynatraceState != null) {
            check.setLastDynatraceState(dynatraceState);
        }
        resolutionCheckRepository.save(check);
    }

    private boolean isExpired(ResolutionCheckEntity check) {
        Instant deadline = check.getStartedAt().plus(properties.getMaxDurationMinutes(), ChronoUnit.MINUTES);
        return Instant.now().isAfter(deadline);
    }
}
