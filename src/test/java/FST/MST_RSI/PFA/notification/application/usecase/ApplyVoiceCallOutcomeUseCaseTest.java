package FST.MST_RSI.PFA.notification.application.usecase;

import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.audit.application.service.AuditRecorder;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyVoiceCallOutcomeUseCaseTest {

    @Mock
    private VoiceCallSessionJpaRepository sessionRepository;
    @Mock
    private NotificationRepositoryPort notificationRepositoryPort;
    @Mock
    private AlertRepositoryPort alertRepositoryPort;
    @Mock
    private RoutingExecutionRepository routingExecutionRepository;
    @Mock
    private RoutingEscalationEngine routingEscalationEngine;
    @Mock
    private AuditRecorder auditRecorder;
    @Mock
    private FST.MST_RSI.PFA.notification.application.service.VoiceCallNarrative voiceCallNarrative;
    @Mock
    private FST.MST_RSI.PFA.notification.application.service.RecordPersonVoipContactUseCase recordPersonVoipContactUseCase;

    private ApplyVoiceCallOutcomeUseCase useCase;
    private final UUID sessionId = UUID.randomUUID();
    private final UUID executionId = UUID.randomUUID();
    private final UUID notificationId = UUID.randomUUID();
    private final String adminChannel = "admin-ch";
    private final String supervisorChannel = "sup-ch";

    @BeforeEach
    void setUp() {
        useCase = new ApplyVoiceCallOutcomeUseCase(
                sessionRepository,
                notificationRepositoryPort,
                alertRepositoryPort,
                routingExecutionRepository,
                routingEscalationEngine,
                auditRecorder,
                new LiveManualCallTracker(),
                voiceCallNarrative,
                recordPersonVoipContactUseCase
        );
        lenient().when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(voiceCallNarrative.describe(any(), any(), any(), any())).thenReturn("Appel VoIP vers destinataire.");
    }

    @Test
    void answeredCompletesRoutingAndAcknowledgesNotification() {
        VoiceCallSessionEntity session = session();
        when(sessionRepository.findByAnyChannelId(adminChannel)).thenReturn(Optional.of(session));
        RoutingExecutionEntity execution = RoutingExecutionEntity.create(
                executionId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), 1, RoutingExecutionStatus.AWAITING_ESCALATION, Instant.now()
        );
        when(routingExecutionRepository.findById(executionId)).thenReturn(Optional.of(execution));

        useCase.answered(adminChannel);

        assertThat(session.getOutcome()).isEqualTo(VoiceCallOutcome.ANSWERED.name());
        assertThat(session.getAnsweredAt()).isNotNull();
        verify(notificationRepositoryPort).updateStatus(notificationId, NotificationStatus.ACKNOWLEDGED);
        verify(routingEscalationEngine).complete(execution, "Appel VoIP répondu — prise en charge");
    }

    @Test
    void finishedIsIdempotentWhenAlreadyEnded() {
        VoiceCallSessionEntity session = session();
        session.setEndedAt(Instant.now());
        session.setOutcome(VoiceCallOutcome.HANGUP.name());
        when(sessionRepository.findByAnyChannelId(supervisorChannel)).thenReturn(Optional.of(session));

        useCase.finished(supervisorChannel, VoiceCallOutcome.HANGUP, 16, "OPS");

        verify(routingEscalationEngine, never()).complete(any(), any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void opsHangupAfterAnswerKeepsPriseEnCharge() {
        VoiceCallSessionEntity session = session();
        session.setAnsweredAt(Instant.now().minusSeconds(12));
        session.setOutcome(VoiceCallOutcome.ANSWERED.name());
        when(sessionRepository.findByAnyChannelId(adminChannel)).thenReturn(Optional.of(session));
        RoutingExecutionEntity execution = RoutingExecutionEntity.create(
                executionId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), 1, RoutingExecutionStatus.AWAITING_ESCALATION, Instant.now()
        );
        when(routingExecutionRepository.findById(executionId)).thenReturn(Optional.of(execution));

        useCase.finished(adminChannel, VoiceCallOutcome.HANGUP, 16, "OPS");

        assertThat(session.getEndedAt()).isNotNull();
        assertThat(session.getHangupSource()).isEqualTo("OPS");
        verify(routingEscalationEngine).complete(eq(execution), any());
        verify(notificationRepositoryPort).updateStatus(notificationId, NotificationStatus.ACKNOWLEDGED);
    }

    @Test
    void noAnswerDoesNotCompleteRouting() {
        VoiceCallSessionEntity session = session();
        when(sessionRepository.findByAnyChannelId(adminChannel)).thenReturn(Optional.of(session));

        useCase.finished(adminChannel, VoiceCallOutcome.NO_ANSWER, 19, "OPS");

        verify(routingEscalationEngine, never()).complete(any(), any());
        verify(notificationRepositoryPort).updateStatus(notificationId, NotificationStatus.FAILED);
    }

    private VoiceCallSessionEntity session() {
        VoiceCallSessionEntity session = new VoiceCallSessionEntity();
        session.setId(sessionId);
        session.setNotificationId(notificationId);
        session.setRoutingExecutionId(executionId);
        session.setProviderCallId(adminChannel);
        session.setSupervisorChannelId(supervisorChannel);
        session.setExtension("1002");
        session.setStartedAt(Instant.now().minusSeconds(20));
        session.setOutcome("RINGING");
        return session;
    }
}
