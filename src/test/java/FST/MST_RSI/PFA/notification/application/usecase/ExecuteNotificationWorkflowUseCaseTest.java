package FST.MST_RSI.PFA.notification.application.usecase;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.model.NotificationState;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationCategory;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationStatus;
import FST.MST_RSI.PFA.common.domain.vo.Confidence;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonRepository;
import FST.MST_RSI.PFA.notification.application.service.EmailMessageComposer;
import FST.MST_RSI.PFA.notification.application.service.SmsKafkaPayloadBuilder;
import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryResult;
import FST.MST_RSI.PFA.notification.domain.model.NotificationRecord;
import FST.MST_RSI.PFA.notification.domain.model.NotificationStatus;
import FST.MST_RSI.PFA.notification.domain.model.NotificationType;
import FST.MST_RSI.PFA.notification.domain.model.SmsNotificationRequest;
import FST.MST_RSI.PFA.notification.domain.port.EmailNotificationPort;
import FST.MST_RSI.PFA.notification.domain.port.NotificationRepositoryPort;
import FST.MST_RSI.PFA.notification.domain.port.SmsNotificationPort;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.NotificationTemplateEntity;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.NotificationTemplateJpaRepository;
import FST.MST_RSI.PFA.routingengine.domain.model.PolicyOrigin;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingDecision;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingStepDefinition;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessDecision;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleOrigin;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecuteNotificationWorkflowUseCaseTest {

    @Mock
    private EmailNotificationPort emailNotificationPort;
    @Mock
    private SmsNotificationPort smsNotificationPort;
    @Mock
    private NotificationRepositoryPort notificationRepositoryPort;
    @Mock
    private NotificationTemplateJpaRepository templateRepository;
    @Mock
    private EmailMessageComposer emailMessageComposer;
    @Mock
    private SmsKafkaPayloadBuilder smsKafkaPayloadBuilder;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private AlertRepositoryPort alertRepositoryPort;

    private ExecuteNotificationWorkflowUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExecuteNotificationWorkflowUseCase(
                emailNotificationPort,
                smsNotificationPort,
                notificationRepositoryPort,
                templateRepository,
                emailMessageComposer,
                smsKafkaPayloadBuilder,
                personRepository,
                alertRepositoryPort
        );
    }

    @Test
    void sendsEmailWhenChannelIsEmail() {
        UUID notificationId = UUID.randomUUID();
        Alert alert = sampleAlert();
        RoutingDecision routingDecision = emailRoutingDecision();
        when(templateRepository.findByCodeAndActiveTrue("DEFAULT-EMAIL-ALERT")).thenReturn(Optional.of(new NotificationTemplateEntity()));
        when(emailMessageComposer.renderSubject(any(), any(), any())).thenReturn("Subject");
        when(emailMessageComposer.renderBody(any(), any(), any(), any())).thenReturn("Body");
        when(notificationRepositoryPort.createPending(any(), any(), eq(NotificationType.EMAIL), any(), anyString()))
                .thenReturn(notificationRecord(notificationId));
        when(emailNotificationPort.send(any())).thenReturn(NotificationDeliveryResult.sent("msg-1"));
        when(alertRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.execute(command(alert, routingDecision));

        assertThat(result.outcome()).isEqualTo("EMAIL_SENT");
        assertThat(alert.getNotificationState()).isEqualTo(NotificationState.ENVOYEE);
    }

    @Test
    void publishesSmsWhenChannelIsSms() {
        UUID notificationId = UUID.randomUUID();
        UUID personId = UUID.randomUUID();
        Alert alert = sampleAlert();
        RoutingDecision routingDecision = smsRoutingDecision(personId);
        PersonEntity person = org.mockito.Mockito.mock(PersonEntity.class);
        org.mockito.Mockito.when(person.getPhone()).thenReturn("1001");
        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(smsKafkaPayloadBuilder.fromAlertContext(any(), any(), any(), any(), anyString(), any()))
                .thenReturn(new SmsNotificationRequest(
                        alert.getId().value(), UUID.randomUUID(), personId, "1001", "Jane",
                        "PayCore", "CPU", "CRITICAL", "P-1", "PayCore", "corr"
                ));
        when(notificationRepositoryPort.createPending(any(), any(), eq(NotificationType.SMS), eq(personId), eq("1001")))
                .thenReturn(notificationRecord(notificationId));
        when(smsNotificationPort.send(any())).thenReturn(NotificationDeliveryResult.sent("0@0"));
        when(alertRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.execute(command(alert, routingDecision));

        assertThat(result.outcome()).isEqualTo("SMS_SENT");
        assertThat(alert.getNotificationState()).isEqualTo(NotificationState.ENVOYEE);
        verify(notificationRepositoryPort).updateStatus(notificationId, NotificationStatus.SENT);
    }

    @Test
    void defersVoipChannelWithoutMarkingEmailSent() {
        UUID notificationId = UUID.randomUUID();
        Alert alert = sampleAlert();
        when(notificationRepositoryPort.createPending(any(), any(), eq(NotificationType.VOIP), any(), anyString()))
                .thenReturn(notificationRecord(notificationId));
        when(alertRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.execute(command(alert, voipRoutingDecision()));

        assertThat(result.outcome()).isEqualTo("DEFERRED_VOIP");
        assertThat(alert.getNotificationState()).isEqualTo(NotificationState.EN_COURS);
    }

    @Test
    void skipsWhenHumanValidationRequired() {
        Alert alert = sampleAlert();
        BusinessDecision decision = new BusinessDecision(
                UUID.randomUUID(), "RULE", RuleOrigin.DEFAULT, true, false,
                null, null, null, List.of(), null, List.of()
        );

        var result = useCase.execute(new ExecuteNotificationWorkflowUseCase.NotificationWorkflowCommand(
                alert, sampleClassification(), decision, emailRoutingDecision()
        ));

        assertThat(result.outcome()).isEqualTo("SKIPPED");
    }

    private ExecuteNotificationWorkflowUseCase.NotificationWorkflowCommand command(
            Alert alert,
            RoutingDecision routingDecision
    ) {
        return new ExecuteNotificationWorkflowUseCase.NotificationWorkflowCommand(
                alert, sampleClassification(), routingBusinessDecision(), routingDecision
        );
    }

    private static Alert sampleAlert() {
        return Alert.createNew(
                "P-123", "CPU saturation", "PayCore", "PROD", "CRITICAL", "APPLICATION",
                "OPEN", "http://dynatrace/problem/1", "host-1", "{}", Instant.now()
        );
    }

    private static ClassificationResult sampleClassification() {
        return new ClassificationResult(
                ClassificationCategory.RESOURCE_CONTENTION, "CPU", new Confidence(0.9),
                "PayCore", "Paiements", "Pilotage", "Core", "s", "c", "j", List.of(), false,
                ClassificationStatus.SUCCESS, null, null
        );
    }

    private static BusinessDecision routingBusinessDecision() {
        return new BusinessDecision(
                UUID.randomUUID(), "RULE", RuleOrigin.DEFAULT, false, true,
                UUID.randomUUID(), "PayCore", null, List.of(), null, List.of("TRIGGER_ROUTING")
        );
    }

    private static RoutingDecision emailRoutingDecision() {
        return routingDecision("tam@example.com", "EMAIL");
    }

    private static RoutingDecision smsRoutingDecision(UUID personId) {
        return new RoutingDecision(
                UUID.randomUUID(), UUID.randomUUID(), "POLICY", PolicyOrigin.DEFAULT,
                personId, null, "Jane Doe", UUID.randomUUID(),
                new RoutingStepDefinition(UUID.randomUUID(), 1, "NOTIFY", "TAM", "SOLUTION", "SMS", 0),
                List.of(), "STARTED"
        );
    }

    private static RoutingDecision voipRoutingDecision() {
        return routingDecision("tam@example.com", "VOIP");
    }

    private static RoutingDecision routingDecision(String email, String channel) {
        return new RoutingDecision(
                UUID.randomUUID(), UUID.randomUUID(), "POLICY", PolicyOrigin.DEFAULT,
                UUID.randomUUID(), email, "Jane Doe", UUID.randomUUID(),
                new RoutingStepDefinition(UUID.randomUUID(), 1, "VOICE_CALL", "TAM", "SOLUTION", channel, 0),
                List.of(), "STARTED"
        );
    }

    private static NotificationRecord notificationRecord(UUID id) {
        return new NotificationRecord(
                id, UUID.randomUUID(), UUID.randomUUID(), NotificationType.EMAIL,
                NotificationStatus.PENDING, UUID.randomUUID(), "dest", Instant.now()
        );
    }
}
