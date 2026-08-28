package FST.MST_RSI.PFA.routingengine.domain.service;

import FST.MST_RSI.PFA.routingengine.domain.model.PolicyOrigin;
import FST.MST_RSI.PFA.routingengine.domain.model.ResolvedPerson;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingContext;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingExecutionStatus;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingPolicy;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingStepDefinition;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionEntity;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingHistoryRepository;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingEscalationEngineTest {

    @Mock
    private PersonResolver personResolver;

    @Mock
    private RoutingExecutionRepository routingExecutionRepository;

    @Mock
    private RoutingHistoryRepository routingHistoryRepository;

    private RoutingEscalationEngine engine;

    private final UUID executionId = UUID.randomUUID();
    private final UUID solutionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        engine = new RoutingEscalationEngine(personResolver, routingExecutionRepository, routingHistoryRepository);
        lenient().when(routingExecutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(routingHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void scheduleNextStepSetsAwaitingEscalation() {
        RoutingExecutionEntity execution = execution(1, RoutingExecutionStatus.STARTED, 0);
        RoutingStepDefinition step = step(1, "VOICE_CALL", 300);

        engine.scheduleNextStep(execution, step);

        assertThat(execution.getRoutingStatus()).isEqualTo(RoutingExecutionStatus.AWAITING_ESCALATION);
        assertThat(execution.getNextEscalationAt()).isNotNull();
        verify(routingExecutionRepository).save(execution);
    }

    @Test
    void advanceStepRetryKeepsCandidateIndex() {
        UUID personId = UUID.randomUUID();
        RoutingExecutionEntity execution = execution(1, RoutingExecutionStatus.AWAITING_ESCALATION, 0);
        RoutingPolicy policy = policy(
                step(1, "VOICE_CALL", 300),
                step(2, "VOICE_RETRY", 120)
        );
        when(personResolver.resolve(any(), any())).thenReturn(List.of(
                new ResolvedPerson(personId, "Jane Doe", "jane@example.com", "TAM", solutionId, true)
        ));

        Optional<RoutingEscalationEngine.EscalationAdvanceResult> result =
                engine.advanceStep(execution, policy, sampleContext());

        assertThat(result).isPresent();
        assertThat(result.get().step().stepOrder()).isEqualTo(2);
        assertThat(execution.getCandidateIndex()).isZero();
        assertThat(execution.getSelectedPersonId()).isEqualTo(personId);
        assertThat(execution.getRoutingStatus()).isEqualTo(RoutingExecutionStatus.STARTED);
    }

    @Test
    void advanceStepNextPersonIncrementsCandidateIndex() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        RoutingExecutionEntity execution = execution(2, RoutingExecutionStatus.AWAITING_ESCALATION, 0);
        RoutingPolicy policy = policy(
                step(1, "VOICE_CALL", 300),
                step(2, "VOICE_RETRY", 120),
                step(3, "NEXT_PERSON", 60)
        );
        when(personResolver.resolve(any(), any())).thenReturn(List.of(
                new ResolvedPerson(first, "Jane Doe", "jane@example.com", "TAM", solutionId, true),
                new ResolvedPerson(second, "John Doe", "john@example.com", "TAM", solutionId, false)
        ));

        Optional<RoutingEscalationEngine.EscalationAdvanceResult> result =
                engine.advanceStep(execution, policy, sampleContext());

        assertThat(result).isPresent();
        assertThat(result.get().step().stepOrder()).isEqualTo(3);
        assertThat(execution.getCandidateIndex()).isEqualTo(1);
        assertThat(execution.getSelectedPersonId()).isEqualTo(second);
    }

    @Test
    void advanceStepCompletesWhenNoFurtherSteps() {
        RoutingExecutionEntity execution = execution(3, RoutingExecutionStatus.AWAITING_ESCALATION, 0);
        RoutingPolicy policy = policy(
                step(1, "VOICE_CALL", 300),
                step(2, "VOICE_RETRY", 120),
                step(3, "NEXT_PERSON", 60)
        );

        Optional<RoutingEscalationEngine.EscalationAdvanceResult> result =
                engine.advanceStep(execution, policy, sampleContext());

        assertThat(result).isEmpty();
        assertThat(execution.getRoutingStatus()).isEqualTo(RoutingExecutionStatus.COMPLETED);
        assertThat(execution.getFinishedAt()).isNotNull();
    }

    @Test
    void advanceStepReturnsNoPersonWhenCandidatesExhausted() {
        RoutingExecutionEntity execution = execution(2, RoutingExecutionStatus.AWAITING_ESCALATION, 0);
        RoutingPolicy policy = policy(
                step(1, "VOICE_CALL", 300),
                step(2, "VOICE_RETRY", 120),
                step(3, "NEXT_PERSON", 60)
        );
        when(personResolver.resolve(any(), any())).thenReturn(List.of());

        Optional<RoutingEscalationEngine.EscalationAdvanceResult> result =
                engine.advanceStep(execution, policy, sampleContext());

        assertThat(result).isPresent();
        assertThat(result.get().routingStatus()).isEqualTo(RoutingExecutionStatus.NO_PERSON);
        assertThat(execution.getRoutingStatus()).isEqualTo(RoutingExecutionStatus.NO_PERSON);
    }

    @Test
    void completeIsIdempotent() {
        RoutingExecutionEntity execution = execution(1, RoutingExecutionStatus.COMPLETED, 0);
        execution.setFinishedAt(Instant.now().minusSeconds(10));

        engine.complete(execution, "duplicate");

        assertThat(execution.getRoutingStatus()).isEqualTo(RoutingExecutionStatus.COMPLETED);
        verify(routingExecutionRepository).save(execution);
    }

    @Test
    void advanceStepDoesNothingWhenAlreadyCompleted() {
        RoutingExecutionEntity execution = execution(1, RoutingExecutionStatus.COMPLETED, 0);
        RoutingPolicy policy = policy(step(1, "VOICE_CALL", 300), step(2, "VOICE_CALL", 120));

        Optional<RoutingEscalationEngine.EscalationAdvanceResult> result =
                engine.advanceStep(execution, policy, sampleContext());

        assertThat(result).isEmpty();
        assertThat(execution.getRoutingStatus()).isEqualTo(RoutingExecutionStatus.COMPLETED);
    }

    @Test
    void expireMarksExecutionExpired() {
        RoutingExecutionEntity execution = execution(1, RoutingExecutionStatus.AWAITING_ESCALATION, 0);

        engine.expire(execution);

        assertThat(execution.getRoutingStatus()).isEqualTo(RoutingExecutionStatus.EXPIRED);
        assertThat(execution.getFinishedAt()).isNotNull();
        ArgumentCaptor<RoutingExecutionEntity> captor = ArgumentCaptor.forClass(RoutingExecutionEntity.class);
        verify(routingExecutionRepository).save(captor.capture());
        assertThat(captor.getValue().getNextEscalationAt()).isNull();
    }

    private RoutingExecutionEntity execution(int currentStep, String status, int candidateIndex) {
        RoutingExecutionEntity entity = RoutingExecutionEntity.create(
                executionId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                solutionId,
                UUID.randomUUID(),
                currentStep,
                status,
                Instant.now().minusSeconds(60)
        );
        entity.setCandidateIndex(candidateIndex);
        return entity;
    }

    private RoutingPolicy policy(RoutingStepDefinition... steps) {
        return new RoutingPolicy(
                UUID.randomUUID(),
                "DEFAULT-VOICE-ESCALATION",
                "Default",
                "Desc",
                true,
                1000,
                PolicyOrigin.DEFAULT,
                List.of(steps)
        );
    }

    private static RoutingStepDefinition step(int order, String action, int delaySeconds) {
        return new RoutingStepDefinition(
                UUID.randomUUID(), order, action, "TAM", "SOLUTION", "VOIP", delaySeconds
        );
    }

    private RoutingContext sampleContext() {
        return new RoutingContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                solutionId,
                "PayCore",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null
        );
    }
}
