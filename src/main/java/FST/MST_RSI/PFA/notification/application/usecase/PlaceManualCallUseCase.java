package FST.MST_RSI.PFA.notification.application.usecase;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.audit.application.service.AuditRecorder;
import FST.MST_RSI.PFA.audit.domain.model.AuditAction;
import FST.MST_RSI.PFA.audit.domain.model.AuditRecord;
import FST.MST_RSI.PFA.common.exception.BusinessException;
import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonRepository;
import FST.MST_RSI.PFA.notification.application.dto.ManualCallRequest;
import FST.MST_RSI.PFA.notification.application.dto.ManualCallResult;
import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryResult;
import FST.MST_RSI.PFA.notification.domain.model.NotificationRecord;
import FST.MST_RSI.PFA.notification.domain.model.NotificationStatus;
import FST.MST_RSI.PFA.notification.domain.model.NotificationType;
import FST.MST_RSI.PFA.notification.domain.model.VoiceCallRequest;
import FST.MST_RSI.PFA.notification.domain.port.NotificationRepositoryPort;
import FST.MST_RSI.PFA.notification.domain.port.VoiceCallPort;
import FST.MST_RSI.PFA.notification.infrastructure.config.VoipNotificationProperties;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionEntity;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionJpaRepository;
import FST.MST_RSI.PFA.notification.infrastructure.voip.asterisk.AsteriskAriClient;
import FST.MST_RSI.PFA.notification.infrastructure.voip.asterisk.AsteriskAriEventListener;
import FST.MST_RSI.PFA.notification.application.service.LiveManualCallTracker;
import FST.MST_RSI.PFA.notification.application.dto.VoiceCallSessionStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PlaceManualCallUseCase {

    private static final Logger log = LoggerFactory.getLogger(PlaceManualCallUseCase.class);

    /** Default extension for supervisor WebRTC phone (PhonePage). */
    private static final String DEFAULT_SUPERVISOR_EXTENSION = "9001";

    private final PersonRepository personRepository;
    private final AlertRepositoryPort alertRepositoryPort;
    private final NotificationRepositoryPort notificationRepositoryPort;
    private final ObjectProvider<VoiceCallPort> voiceCallPortProvider;
    private final VoipNotificationProperties voipNotificationProperties;
    private final AuditRecorder auditRecorder;
    private final VoiceCallSessionJpaRepository sessionRepository;
    // Optional — only present when Asterisk adapter is active
    private final ObjectProvider<AsteriskAriClient> ariClientProvider;
    private final ObjectProvider<AsteriskAriEventListener> ariListenerProvider;
    private final LiveManualCallTracker liveManualCallTracker;

    public PlaceManualCallUseCase(
            PersonRepository personRepository,
            AlertRepositoryPort alertRepositoryPort,
            NotificationRepositoryPort notificationRepositoryPort,
            ObjectProvider<VoiceCallPort> voiceCallPortProvider,
            VoipNotificationProperties voipNotificationProperties,
            AuditRecorder auditRecorder,
            VoiceCallSessionJpaRepository sessionRepository,
            ObjectProvider<AsteriskAriClient> ariClientProvider,
            ObjectProvider<AsteriskAriEventListener> ariListenerProvider,
            LiveManualCallTracker liveManualCallTracker
    ) {
        this.personRepository = personRepository;
        this.alertRepositoryPort = alertRepositoryPort;
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.voiceCallPortProvider = voiceCallPortProvider;
        this.voipNotificationProperties = voipNotificationProperties;
        this.auditRecorder = auditRecorder;
        this.sessionRepository = sessionRepository;
        this.ariClientProvider = ariClientProvider;
        this.ariListenerProvider = ariListenerProvider;
        this.liveManualCallTracker = liveManualCallTracker;
    }

    @Transactional
    public ManualCallResult execute(ManualCallRequest request) {
        PersonEntity person = personRepository.findById(request.personId())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + request.personId()));
        if (person.getPhone() == null || person.getPhone().isBlank()) {
            throw new BusinessException("NO_RECIPIENT_PHONE", "Cette personne n'a pas de numéro de téléphone.");
        }

        UUID alertId = resolveAlertId(request.alertId());

        // Create the notification record FIRST so its ID can be used as correlationId
        UUID notificationId = null;
        NotificationRecord notification = null;
        if (alertId != null) {
            notification = notificationRepositoryPort.createPending(
                    alertId,
                    null,
                    NotificationType.VOIP,
                    person.getId(),
                    person.getPhone(),
                    "MANUAL",
                    person.getId()
            );
            notificationId = notification.id();
        }

        // Determine if we have a live bridge (supervisor extension provided and VoIP enabled)
        String supervisorExt = request.supervisorExtension() != null && !request.supervisorExtension().isBlank()
                ? request.supervisorExtension().trim()
                : null;
        boolean liveMode = voipNotificationProperties.isEnabled() && supervisorExt != null;

        NotificationDeliveryResult delivery;
        UUID voiceSessionId = null;
        String supervisorChannelId = null;
        String adminChannelId = null;

        if (liveMode) {
            BridgedCallResult bridged = placeBridgedCall(person, supervisorExt, notificationId, alertId);
            delivery = bridged.delivery();
            voiceSessionId = bridged.sessionId();
            supervisorChannelId = bridged.supervisorChannelId();
            adminChannelId = bridged.adminChannelId();
        } else {
            VoiceCallRequest callRequest = buildTtsCallRequest(person, notificationId, alertId);
            delivery = placeCall(callRequest);
        }

        // Update notification status
        if (notification != null) {
            notificationRepositoryPort.recordAttempt(
                    notification.id(),
                    1,
                    "manual-voip",
                    delivery.success() ? NotificationStatus.SENT : NotificationStatus.FAILED,
                    delivery.providerMessageId(),
                    delivery.errorMessage()
            );
            notificationRepositoryPort.updateStatus(
                    notification.id(),
                    delivery.success() ? NotificationStatus.SENT : NotificationStatus.FAILED
            );
        }

        String status = delivery.success() ? "SENT" : "FAILED";
        String detail = delivery.success()
                ? "Appel manuel (bridge live superviseur ↔ " + person.getFullName() + ")"
                : delivery.errorMessage();

        recordAudit(person, alertId, notificationId, status, detail);

        log.info("Manual VoIP call person={} alert={} status={} liveMode={}", person.getId(), alertId, status, liveMode);
        return new ManualCallResult(
                status,
                person.getFullName(),
                person.getPhone(),
                delivery.providerMessageId(),
                detail,
                notificationId,
                alertId,
                liveMode,
                voiceSessionId,
                supervisorChannelId,
                adminChannelId
        );
    }

    // ─── Bridge mode: originate supervisor channel + admin channel, then bridge ───

    private BridgedCallResult placeBridgedCall(
            PersonEntity person,
            String supervisorExt,
            UUID notificationId,
            UUID alertId
    ) {
        AsteriskAriClient ariClient = ariClientProvider.getIfAvailable();
        AsteriskAriEventListener ariListener = ariListenerProvider.getIfAvailable();
        if (ariClient == null || ariListener == null) {
            log.warn("[VOICE] Asterisk ARI not available for bridged call — falling back to TTS");
            return new BridgedCallResult(placeCall(buildTtsCallRequest(person, notificationId, alertId)), null, null, null);
        }

        try {
            UUID sessionUuid = UUID.randomUUID();
            String sessionId = sessionUuid.toString();
            String supervisorChannelId = AsteriskAriClient.newChannelId();
            String adminChannelId = AsteriskAriClient.newChannelId();
            String commonArgs = "live=true,session=" + sessionId
                    + (notificationId != null ? ",notification=" + notificationId : "");

            ariListener.registerBridgeSession(sessionId, supervisorChannelId, adminChannelId);

            String adminExtension = FST.MST_RSI.PFA.notification.domain.service.SipEndpointMapper
                    .extensionFromPhone(person.getPhone());

            VoiceCallSessionEntity session = new VoiceCallSessionEntity();
            session.setId(sessionUuid);
            session.setNotificationId(notificationId);
            session.setAlertId(alertId);
            session.setPersonId(person.getId());
            session.setExtension(adminExtension);
            session.setProviderCallId(adminChannelId);
            session.setSupervisorChannelId(supervisorChannelId);
            session.setSupervisorExtension(supervisorExt);
            session.setRecordingName("manual-" + sessionId);
            session.setSoundName("manual-" + sessionId);
            session.setOutcome("INITIATED");
            session.setStartedAt(Instant.now());
            session.setLiveConversation(true);
            sessionRepository.save(session);

            liveManualCallTracker.started(LiveManualCallTracker.snapshot(
                    sessionUuid,
                    "INITIATED",
                    true,
                    supervisorChannelId,
                    adminChannelId,
                    supervisorExt,
                    session.getStartedAt(),
                    null,
                    null,
                    null
            ));

            ariClient.originate(
                    "PJSIP/" + supervisorExt,
                    commonArgs + ",role=supervisor",
                    voipNotificationProperties.getTimeoutSeconds(),
                    supervisorChannelId
            );
            log.info("[VOICE] Supervisor channel originated ext={} channelId={}", supervisorExt, supervisorChannelId);

            ariClient.originate(
                    FST.MST_RSI.PFA.notification.domain.service.SipEndpointMapper.pjsipEndpoint(adminExtension),
                    commonArgs + ",role=admin",
                    voipNotificationProperties.getTimeoutSeconds(),
                    adminChannelId
            );
            log.info("[VOICE] Admin channel originated ext={} channelId={}", adminExtension, adminChannelId);

            return new BridgedCallResult(
                    NotificationDeliveryResult.sent(adminChannelId),
                    sessionUuid,
                    supervisorChannelId,
                    adminChannelId
            );
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
            if (msg.contains("Allocation failed")) {
                msg = "Impossible de joindre l'extension superviseur (" + supervisorExt + ") : votre téléphone VoIP n'est pas connecté. Veuillez vous connecter sur la page Téléphone VoIP.";
            }
            log.warn("[VOICE] Bridged call failed: {}", msg);
            return new BridgedCallResult(NotificationDeliveryResult.failed(msg), null, null, null);
        }
    }

    public void hangup(String channelId) {
        hangup(null, channelId);
    }

    public void hangup(UUID sessionId, String channelId) {
        AsteriskAriEventListener ariListener = ariListenerProvider.getIfAvailable();
        AsteriskAriClient ariClient = ariClientProvider.getIfAvailable();

        VoiceCallSessionEntity session = null;
        if (sessionId != null) {
            session = sessionRepository.findById(sessionId).orElse(null);
        }
        if (session == null && channelId != null && !channelId.isBlank()) {
            session = sessionRepository.findByAnyChannelId(channelId).orElse(null);
        }

        if (session != null && ariListener != null) {
            ariListener.terminateSession(session.getId().toString());
            log.info("[VOICE] Explicit hangup for session={}", session.getId());
            return;
        }
        if (ariListener != null && channelId != null && !channelId.isBlank()) {
            ariListener.terminateByChannel(channelId);
            log.info("[VOICE] Explicit hangup for channelId={}", channelId);
            return;
        }
        if (ariClient != null && session != null) {
            if (session.getRecordingName() != null) {
                ariClient.stopRecording(session.getRecordingName());
            }
            if (session.getBridgeId() != null) {
                ariClient.destroyBridge(session.getBridgeId());
            }
            ariClient.hangup(session.getSupervisorChannelId());
            ariClient.hangup(session.getProviderCallId());
            return;
        }
        if (ariClient != null && channelId != null && !channelId.isBlank()) {
            ariClient.hangup(channelId);
        }
    }

    public VoiceCallSessionStatusDto status(UUID sessionId) {
        VoiceCallSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Voice call session not found: " + sessionId));
        return LiveManualCallTracker.snapshot(
                session.getId(),
                session.getOutcome(),
                session.getEndedAt() == null,
                session.getSupervisorChannelId(),
                session.getProviderCallId(),
                session.getSupervisorExtension(),
                session.getStartedAt(),
                session.getAnsweredAt(),
                session.getEndedAt(),
                session.getHangupCause()
        );
    }

    private record BridgedCallResult(
            NotificationDeliveryResult delivery,
            UUID sessionId,
            String supervisorChannelId,
            String adminChannelId
    ) {
    }

    // ─── TTS fallback (no supervisor bridge) ────────────────────────────────────

    private VoiceCallRequest buildTtsCallRequest(PersonEntity person, UUID notificationId, UUID alertId) {
        return new VoiceCallRequest(
                alertId,
                null,
                person.getId(),
                person.getPhone(),
                person.getFullName(),
                "LIVE_CONVERSATION",
                null,
                null,
                notificationId != null ? notificationId.toString() : null,
                true
        );
    }

    private NotificationDeliveryResult placeCall(VoiceCallRequest callRequest) {
        VoiceCallPort port = voiceCallPortProvider.getIfAvailable();
        if (voipNotificationProperties.isEnabled() && port != null) {
            return port.call(callRequest);
        }
        String callId = UUID.randomUUID().toString();
        log.info("Manual VoIP simulated callId={} (adapter disabled or missing)", callId);
        return NotificationDeliveryResult.sent(callId);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Returns the supervisor extension to use for the bridge.
     * Falls back to DEFAULT_SUPERVISOR_EXTENSION if none provided.
     */
    private String resolveSupervisorExtension(String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        // If VoIP is enabled, default to 9001
        return voipNotificationProperties.isEnabled() ? DEFAULT_SUPERVISOR_EXTENSION : null;
    }

    private UUID resolveAlertId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            UUID uuid = UUID.fromString(value);
            return alertRepositoryPort.findById(new AlertId(uuid))
                    .map(alert -> alert.getId().value())
                    .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + value));
        } catch (IllegalArgumentException notUuid) {
            return alertRepositoryPort.findByExternalProblemId(value)
                    .map(Alert::getId)
                    .map(AlertId::value)
                    .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + value));
        }
    }

    private void recordAudit(PersonEntity person, UUID alertId, UUID notificationId, String status, String detail) {
        AuditRecord audit = new AuditRecord(
                AuditAction.MANUAL_CALL,
                alertId,
                null,
                null,
                notificationId,
                null,
                "Person",
                person.getId(),
                detail + " [" + status + "]",
                AuditRecorder.correlationId(alertId != null ? alertId : person.getId()),
                null,
                List.of(
                        new AuditRecord.AuditDetail("callMode", null, "MANUAL"),
                        new AuditRecord.AuditDetail("destination", null, person.getPhone())
                )
        );
        if (TransactionSynchronizationManager.isSynchronizationActive() && notificationId != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    auditRecorder.record(audit);
                }
            });
        } else {
            auditRecorder.record(new AuditRecord(
                    audit.action(), audit.alertId(), audit.llmAnalysisId(),
                    audit.routingExecutionId(), null, audit.actorPersonId(),
                    audit.entityName(), audit.entityId(), audit.description(),
                    audit.correlationId(), audit.ipAddress(), audit.details()
            ));
        }
    }
}
