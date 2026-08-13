package FST.MST_RSI.PFA.routingengine.domain.service;

import FST.MST_RSI.PFA.routingengine.domain.model.PolicyOrigin;
import FST.MST_RSI.PFA.routingengine.domain.model.ResolvedPerson;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingContext;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingDecision;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingPolicy;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingStepDefinition;
import FST.MST_RSI.PFA.routingengine.domain.port.RoutingPolicyRepositoryPort;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingExecutionRepository;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessDecision;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleOrigin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingEngineTest {

    @Mock
    private RoutingPolicyRepositoryPort policyRepositoryPort;

    @Mock
    private PersonResolver personResolver;

    @Mock
    private RoutingExecutionRepository routingExecutionRepository;

    private RoutingEngine routingEngine;

    @BeforeEach
    void setUp() {
        routingEngine = new RoutingEngine(policyRepositoryPort, personResolver, routingExecutionRepository);
        lenient().when(routingExecutionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void skipsRoutingWhenHumanValidationRequired() {
        RoutingDecision decision = routingEngine.buildRoutingDecision(sampleContext(
                businessDecision(true, true, null),
                UUID.randomUUID()
        ));
        assertThat(decision.routingStatus()).isEqualTo("AWAITING_HUMAN_VALIDATION");
    }

    @Test
    void usesDefaultPolicyAndResolvesTam() {
        UUID policyId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        UUID solutionId = UUID.randomUUID();

        when(policyRepositoryPort.findEnabledByOrigin(PolicyOrigin.CONFIGURED)).thenReturn(List.of());
        when(policyRepositoryPort.findEnabledByOrigin(PolicyOrigin.DEFAULT)).thenReturn(List.of(defaultPolicy(policyId)));

        RoutingStepDefinition step = new RoutingStepDefinition(
                UUID.randomUUID(), 1, "VOICE_CALL", "TAM", "SOLUTION", "VOIP", 300
        );
        when(personResolver.resolve(any(), any())).thenReturn(List.of(
                new ResolvedPerson(personId, "Jane Doe", "jane@example.com", "TAM", solutionId, true)
        ));

        RoutingDecision decision = routingEngine.buildRoutingDecision(sampleContext(
                businessDecision(false, true, null),
                solutionId
        ));

        assertThat(decision.routingStatus()).isEqualTo("STARTED");
        assertThat(decision.policyCode()).isEqualTo("DEFAULT-VOICE-ESCALATION");
        assertThat(decision.selectedPersonId()).isEqualTo(personId);
    }

    @Test
    void returnsNoPersonWhenResolverEmpty() {
        UUID policyId = UUID.randomUUID();
        when(policyRepositoryPort.findEnabledByOrigin(PolicyOrigin.CONFIGURED)).thenReturn(List.of());
        when(policyRepositoryPort.findEnabledByOrigin(PolicyOrigin.DEFAULT)).thenReturn(List.of(defaultPolicy(policyId)));
        when(personResolver.resolve(any(), any())).thenReturn(List.of());

        RoutingDecision decision = routingEngine.buildRoutingDecision(sampleContext(
                businessDecision(false, true, null),
                UUID.randomUUID()
        ));

        assertThat(decision.routingStatus()).isEqualTo("NO_PERSON");
    }

    private RoutingPolicy defaultPolicy(UUID policyId) {
        return new RoutingPolicy(
                policyId,
                "DEFAULT-VOICE-ESCALATION",
                "Default",
                "Desc",
                true,
                1000,
                PolicyOrigin.DEFAULT,
                List.of(new RoutingStepDefinition(
                        UUID.randomUUID(), 1, "VOICE_CALL", "TAM", "SOLUTION", "VOIP", 300
                ))
        );
    }

    private BusinessDecision businessDecision(boolean humanValidation, boolean routing, String forcedRole) {
        return new BusinessDecision(
                UUID.randomUUID(),
                "RULE",
                RuleOrigin.DEFAULT,
                humanValidation,
                routing,
                UUID.randomUUID(),
                "PayCore",
                forcedRole,
                List.of(),
                null,
                List.of()
        );
    }

    private RoutingContext sampleContext(BusinessDecision decision, UUID solutionId) {
        return new RoutingContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                decision,
                solutionId,
                "PayCore",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                decision.forcedRole()
        );
    }
}
