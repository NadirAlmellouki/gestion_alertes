package FST.MST_RSI.PFA.notification.application.usecase;

import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.audit.application.service.AuditRecorder;
import FST.MST_RSI.PFA.audit.domain.model.AuditAction;
import FST.MST_RSI.PFA.audit.domain.model.AuditRecord;
import FST.MST_RSI.PFA.notification.application.service.LiveManualCallTracker;
import FST.MST_RSI.PFA.notification.domain.model.NotificationStatus;
import FST.MST_RSI.PFA.notification.domain.model.VoiceCallOutcome;
import FST.MST_RSI.PFA.notification.domain.port.NotificationRepositoryPort;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionEntity;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionJpaRepository;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingExecutionStatus;
import FST.MST_RSI.PFA.routingengine.domain.service.RoutingEscalationEngine;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionEntity;
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
    private final LiveManualCallTracker liveManualCallTracker;

    public ApplyVoiceCallOutcomeUseCase(
            VoiceCallSessionJpaRepository sessionRepository,
            NotificationRepositoryPort notificationRepositoryPort,
            AlertRepositoryPort alertRepositoryPort,
            RoutingExecutionRepository routingExecutionRepository,
            RoutingEscalationEngine routingEscalationEngine,
            AuditRecorder auditRecorder,
            LiveManualCallTracker liveManualCallTracker
    ) {
        this.sessionRepository = sessionRepository;
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.alertRepositoryPort = alertRepositoryPort;
        this.routingExecutionRepository = routingExecutionRepository;
        this.routingEscalationEngine = routingEscalationEngine;
        this.auditRecorder = auditRecorder;
        this.liveManualCallTracker = liveManualCallTracker;
    }

    @Transactional
    public void ringing(String providerCallId) {
        resolve(providerCallId).ifPresent(session -> {
            if (session.getEndedAt() != null) {
                return;
            }
            session.setOutcome(VoiceCallOutcome.RINGING.name());
            if (session.getRingingAt() == null) {
                session.setRingingAt(Instant.now());
            }
            sessionRepository.save(session);
            publish(session, true);
            audit(session, AuditAction.VOICE_CALL_RINGING, "SIP RINGING");
        });
    }

    @Transactional
    public void answered(String providerCallId) {
        resolve(providerCallId).ifPresent(session -> {
            if (session.getEndedAt() != null) {
                return;
            }
            Instant now = Instant.now();
            session.setOutcome(VoiceCallOutcome.ANSWERED.name());
            if (session.getAnsweredAt() == null) {
                session.setAnsweredAt(now);
            }
            sessionRepository.save(session);
            if (session.getNotificationId() != null) {
                notificationRepositoryPort.updateStatus(session.getNotificationId(), NotificationStatus.ACKNOWLEDGED);
            }
            stopEscalationAfterVoipAnswer(session, "Appel VoIP répondu — prise en charge");
            publish(session, true);
            audit(session, AuditAction.VOICE_CALL_ANSWERED, "SIP ANSWERED");
        });
    }

    @Transactional
    public void attachBridge(UUID sessionId, String bridgeId, String recordingName) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setBridgeId(bridgeId);
            session.setRecordingName(recordingName);
            sessionRepository.save(session);
        });
    }

    @Transactional
    public void finished(String providerCallId, VoiceCallOutcome outcome, Integer hangupCause) {
        finished(providerCallId, outcome, hangupCause, null);
    }

    @Transactional
    public void finished(String providerCallId, VoiceCallOutcome outcome, Integer hangupCause, String hangupSource) {
        VoiceCallSessionEntity session = resolve(providerCallId).orElse(null);
        if (session == null) {
            return;
        }
        if (session.getEndedAt() != null) {
            publish(session, false);
            return;
        }
        Instant now = Instant.now();
        session.setOutcome(outcome.name());
        session.setHangupCause(hangupCause);
        session.setHangupSource(hangupSource);
        session.setEndedAt(now);
        Instant start = session.getAnsweredAt() != null ? session.getAnsweredAt() : session.getStartedAt();
        if (start != null) {
            session.setDurationSeconds((int) Duration.between(start, now).toSeconds());
        }
        sessionRepository.save(session);

        boolean answered = session.getAnsweredAt() != null;
        boolean success = answered
                || outcome == VoiceCallOutcome.ANSWERED
                || outcome == VoiceCallOutcome.HANGUP;
        if (session.getNotificationId() != null) {
            notificationRepositoryPort.updateStatus(
                    session.getNotificationId(),
                    success ? (answered ? NotificationStatus.ACKNOWLEDGED : NotificationStatus.SENT)
                            : NotificationStatus.FAILED
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
        if (answered) {
            stopEscalationAfterVoipAnswer(session, "VoIP answered / hangup after audio");
        }

        String action = switch (outcome) {
            case REJECTED -> AuditAction.VOICE_CALL_REJECTED;
            case BUSY -> AuditAction.VOICE_CALL_BUSY;
            case NO_ANSWER -> AuditAction.VOICE_CALL_NO_ANSWER;
            case FAILED -> AuditAction.VOICE_CALL_FAILED;
            default -> AuditAction.VOICE_CALL_HANGUP;
        };
        audit(session, action, "SIP " + outcome + " cause=" + hangupCause);
        publish(session, false);
        log.info("[VOICE] SIP state={} callId={} cause={} source={}", outcome, providerCallId, hangupCause, hangupSource);
    }

    public java.util.Optional<VoiceCallSessionEntity> resolve(String channelId) {
        if (channelId == null || channelId.isBlank()) {
            return java.util.Optional.empty();
        }
        return sessionRepository.findByAnyChannelId(channelId)
                .or(() -> sessionRepository.findByProviderCallId(channelId));
    }

    private void stopEscalationAfterVoipAnswer(VoiceCallSessionEntity session, String reason) {
        UUID routingExecutionId = session.getRoutingExecutionId();
        if (routingExecutionId == null && session.getNotificationId() != null) {
            routingExecutionId = notificationRepositoryPort.findById(session.getNotificationId())
                    .map(FST.MST_RSI.PFA.notification.domain.model.NotificationRecord::routingExecutionId)
                    .orElse(null);
        }
        if (routingExecutionId == null) {
            return;
        }
        RoutingExecutionEntity execution = routingExecutionRepository.findById(routingExecutionId).orElse(null);
        if (execution == null) {
            return;
        }
        if (RoutingExecutionStatus.COMPLETED.equals(execution.getRoutingStatus())
                || RoutingExecutionStatus.EXPIRED.equals(execution.getRoutingStatus())) {
            return;
        }
        routingEscalationEngine.complete(execution, reason);
    }

    private void publish(VoiceCallSessionEntity session, boolean active) {
        liveManualCallTracker.updated(LiveManualCallTracker.snapshot(
                session.getId(),
                session.getOutcome(),
                active && session.getEndedAt() == null,
                session.getSupervisorChannelId(),
                session.getProviderCallId(),
                session.getSupervisorExtension(),
                session.getStartedAt(),
                session.getAnsweredAt(),
                session.getEndedAt(),
                session.getHangupCause()
        ));
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
