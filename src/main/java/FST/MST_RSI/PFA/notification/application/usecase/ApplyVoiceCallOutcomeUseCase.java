package FST.MST_RSI.PFA.notification.application.usecase;

import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.audit.application.service.AuditRecorder;
import FST.MST_RSI.PFA.audit.domain.model.AuditAction;
import FST.MST_RSI.PFA.audit.domain.model.AuditRecord;
import FST.MST_RSI.PFA.notification.domain.model.NotificationStatus;
import FST.MST_RSI.PFA.notification.domain.model.VoiceCallOutcome;
import FST.MST_RSI.PFA.notification.domain.port.NotificationRepositoryPort;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionEntity;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionJpaRepository;
import FST.MST_RSI.PFA.routingengine.domain.service.RoutingEscalationEngine;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ApplyVoiceCallOutcomeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApplyVoiceCallOutcomeUseCase.class);

    private final VoiceCallSessionJpaRepository sessionRepository;
    private final NotificationRepositoryPort notificationRepositoryPort;
    private final AlertRepositoryPort alertRepositoryPort;
    private final RoutingExecutionRepository routingExecutionRepository;
    private final RoutingEscalationEngine routingEscalationEngine;
    private final AuditRecorder auditRecorder;

    public ApplyVoiceCallOutcomeUseCase(
            VoiceCallSessionJpaRepository sessionRepository,
            NotificationRepositoryPort notificationRepositoryPort,
            AlertRepositoryPort alertRepositoryPort,
            RoutingExecutionRepository routingExecutionRepository,
            RoutingEscalationEngine routingEscalationEngine,
            AuditRecorder auditRecorder
    ) {
        this.sessionRepository = sessionRepository;
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.alertRepositoryPort = alertRepositoryPort;
        this.routingExecutionRepository = routingExecutionRepository;
        this.routingEscalationEngine = routingEscalationEngine;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public void ringing(String providerCallId) {
        sessionRepository.findByProviderCallId(providerCallId).ifPresent(session -> {
            session.setOutcome(VoiceCallOutcome.RINGING.name());
            session.setRingingAt(Instant.now());
            sessionRepository.save(session);
            audit(session, AuditAction.VOICE_CALL_RINGING, "SIP RINGING");
        });
    }

    @Transactional
    public void answered(String providerCallId) {
        sessionRepository.findByProviderCallId(providerCallId).ifPresent(session -> {
            session.setOutcome(VoiceCallOutcome.ANSWERED.name());
            session.setAnsweredAt(Instant.now());
            sessionRepository.save(session);
            audit(session, AuditAction.VOICE_CALL_ANSWERED, "SIP ANSWERED");
        });
    }

    @Transactional
    public void finished(String providerCallId, VoiceCallOutcome outcome, Integer hangupCause) {
        VoiceCallSessionEntity session = sessionRepository.findByProviderCallId(providerCallId).orElse(null);
        if (session == null) {
            return;
        }
        Instant now = Instant.now();
        session.setOutcome(outcome.name());
        session.setHangupCause(hangupCause);
        session.setEndedAt(now);
        if (session.getStartedAt() != null) {
            session.setDurationSeconds((int) Duration.between(session.getStartedAt(), now).toSeconds());
        }
        sessionRepository.save(session);

        boolean success = outcome == VoiceCallOutcome.ANSWERED || outcome == VoiceCallOutcome.HANGUP;
        if (session.getNotificationId() != null) {
            notificationRepositoryPort.updateStatus(
                    session.getNotificationId(),
                    success ? NotificationStatus.SENT : NotificationStatus.FAILED
            );
        }
        if (session.getAlertId() != null) {
            alertRepositoryPort.findById(new AlertId(session.getAlertId())).ifPresent(alert -> {
                if (success) {
                    alert.markNotificationSent();
                } else {
                    alert.markNotificationFailed();
                }
                alertRepositoryPort.save(alert);
            });
        }
        if (success && session.getRoutingExecutionId() != null) {
            routingExecutionRepository.findById(session.getRoutingExecutionId())
                    .ifPresent(execution -> routingEscalationEngine.complete(execution, "VoIP answered / hangup after audio"));
        }

        String action = switch (outcome) {
            case REJECTED -> AuditAction.VOICE_CALL_REJECTED;
            case BUSY -> AuditAction.VOICE_CALL_BUSY;
            case NO_ANSWER -> AuditAction.VOICE_CALL_NO_ANSWER;
            case FAILED -> AuditAction.VOICE_CALL_FAILED;
            default -> AuditAction.VOICE_CALL_HANGUP;
        };
        audit(session, action, "SIP " + outcome + " cause=" + hangupCause);
        log.info("[VOICE] SIP state={} callId={} cause={}", outcome, providerCallId, hangupCause);
    }

    private void audit(VoiceCallSessionEntity session, String action, String description) {
        auditRecorder.record(new AuditRecord(
                action,
                session.getAlertId(),
                null,
                session.getRoutingExecutionId(),
                session.getNotificationId(),
                session.getPersonId(),
                "VoiceCallSession",
                session.getId(),
                description + " ext=" + session.getExtension(),
                AuditRecorder.correlationId(session.getAlertId() != null ? session.getAlertId() : session.getId()),
                null,
                List.of()
        ));
    }
}
