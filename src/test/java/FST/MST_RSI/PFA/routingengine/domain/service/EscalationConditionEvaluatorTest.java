package FST.MST_RSI.PFA.routingengine.domain.service;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.monitoring.domain.model.DynatraceProblemSnapshot;
import FST.MST_RSI.PFA.monitoring.domain.port.DynatraceProblemPort;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionEntity;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.VoiceCallSessionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscalationConditionEvaluatorTest {

    @Mock
    private VoiceCallSessionJpaRepository voiceCallSessionRepository;
    @Mock
    private DynatraceProblemPort dynatraceProblemPort;

    private EscalationConditionEvaluator evaluator;
    private final UUID executionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        evaluator = new EscalationConditionEvaluator(voiceCallSessionRepository, dynatraceProblemPort);
    }

    @Test
    void stopsWhenVoipWasAnswered() {
        VoiceCallSessionEntity session = new VoiceCallSessionEntity();
        session.setAnsweredAt(Instant.now());
        when(voiceCallSessionRepository.findByRoutingExecutionId(executionId)).thenReturn(List.of(session));
        when(dynatraceProblemPort.fetchProblem("P-123"))
                .thenReturn(Optional.of(new DynatraceProblemSnapshot("P-123", "OPEN")));

        Optional<String> reason = evaluator.stopReason(executionId, openAlert());

        assertThat(reason).isPresent();
        assertThat(reason.get()).contains("VoIP");
    }

    @Test
    void continuesWhenVoipWasNotAnswered() {
        VoiceCallSessionEntity session = new VoiceCallSessionEntity();
        session.setOutcome("NO_ANSWER");
        when(voiceCallSessionRepository.findByRoutingExecutionId(executionId)).thenReturn(List.of(session));
        when(dynatraceProblemPort.fetchProblem("P-123"))
                .thenReturn(Optional.of(new DynatraceProblemSnapshot("P-123", "OPEN")));

        Optional<String> reason = evaluator.stopReason(executionId, openAlert());

        assertThat(reason).isEmpty();
    }

    @Test
    void stopsWhenDynatraceStateIsResolved() {
        Alert alert = Alert.createNew(
                "P-123", "CPU", "PayCore", "PROD", "CRITICAL", "APPLICATION",
                "RESOLVED", "http://x", "host", "{}", Instant.now()
        );

        Optional<String> reason = evaluator.stopReason(executionId, alert);

        assertThat(reason).isPresent();
        assertThat(reason.get()).contains("Dynatrace");
    }

    @Test
    void stopsWhenLiveDynatraceFetchSaysResolved() {
        when(dynatraceProblemPort.fetchProblem("P-123"))
                .thenReturn(Optional.of(new DynatraceProblemSnapshot("P-123", "RESOLVED")));

        Optional<String> reason = evaluator.stopReason(executionId, openAlert());

        assertThat(reason).isPresent();
    }

    private static Alert openAlert() {
        return Alert.createNew(
                "P-123", "CPU", "PayCore", "PROD", "CRITICAL", "APPLICATION",
                "OPEN", "http://x", "host", "{}", Instant.now()
        );
    }
}
