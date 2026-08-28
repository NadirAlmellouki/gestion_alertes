package FST.MST_RSI.PFA.routingengine.domain.service;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.monitoring.domain.model.DynatraceProblemSnapshot;
import FST.MST_RSI.PFA.monitoring.domain.port.DynatraceProblemPort;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionEntity;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionJpaRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class EscalationConditionEvaluator {

    private final VoiceCallSessionJpaRepository voiceCallSessionRepository;
    private final DynatraceProblemPort dynatraceProblemPort;

    public EscalationConditionEvaluator(
            VoiceCallSessionJpaRepository voiceCallSessionRepository,
            DynatraceProblemPort dynatraceProblemPort
    ) {
        this.voiceCallSessionRepository = voiceCallSessionRepository;
        this.dynatraceProblemPort = dynatraceProblemPort;
    }

    public Optional<String> stopReason(UUID routingExecutionId, Alert alert) {
        if (isIncidentResolved(alert)) {
            return Optional.of("Incident Dynatrace résolu — escalade arrêtée");
        }
        if (isVoipAnswered(routingExecutionId)) {
            return Optional.of("Appel VoIP répondu — prise en charge, escalade arrêtée");
        }
        return Optional.empty();
    }

    public boolean isVoipAnswered(UUID routingExecutionId) {
        if (routingExecutionId == null) {
            return false;
        }
        return voiceCallSessionRepository.findByRoutingExecutionId(routingExecutionId).stream()
                .anyMatch(this::countsAsPriseEnCharge);
    }

    public boolean isIncidentResolved(Alert alert) {
        if (alert == null) {
            return false;
        }
        if (alert.getDynatraceState() != null && "RESOLVED".equalsIgnoreCase(alert.getDynatraceState())) {
            return true;
        }
        String problemId = alert.getExternalProblemId();
        if (problemId == null || problemId.isBlank()) {
            return false;
        }
        return dynatraceProblemPort.fetchProblem(problemId)
                .map(DynatraceProblemSnapshot::isResolved)
                .orElse(false);
    }

    private boolean countsAsPriseEnCharge(VoiceCallSessionEntity session) {
        return session.getAnsweredAt() != null;
    }
}
