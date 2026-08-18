package FST.MST_RSI.PFA.notification.application.usecase;

import FST.MST_RSI.PFA.audit.application.service.AuditRecorder;
import FST.MST_RSI.PFA.audit.domain.model.AuditAction;
import FST.MST_RSI.PFA.audit.domain.model.AuditRecord;
import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.monitoring.application.usecase.ScheduleResolutionCheckUseCase;
import FST.MST_RSI.PFA.notification.application.service.EmailMessageComposer;
import FST.MST_RSI.PFA.notification.application.service.SmsKafkaPayloadBuilder;
import FST.MST_RSI.PFA.notification.application.service.VoipMessageComposer;
import FST.MST_RSI.PFA.notification.domain.model.NotificationChannel;
import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryRequest;
import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryResult;
import FST.MST_RSI.PFA.notification.domain.model.NotificationRecord;
import FST.MST_RSI.PFA.notification.domain.model.NotificationStatus;
import FST.MST_RSI.PFA.notification.domain.model.NotificationType;
import FST.MST_RSI.PFA.notification.domain.model.SmsNotificationRequest;
import FST.MST_RSI.PFA.notification.domain.model.VoiceCallRequest;
import FST.MST_RSI.PFA.notification.domain.port.EmailNotificationPort;
import FST.MST_RSI.PFA.notification.domain.port.NotificationRepositoryPort;
import FST.MST_RSI.PFA.notification.domain.port.SmsNotificationPort;
import FST.MST_RSI.PFA.notification.domain.port.VoiceCallPort;
import FST.MST_RSI.PFA.notification.infrastructure.config.VoipNotificationProperties;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonRepository;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.NotificationTemplateEntity;
import FST.MST_RSI.PFA.notification.infrastructure.persistence.NotificationTemplateJpaRepository;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingDecision;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessDecision;
import FST.MST_RSI.PFA.voicemessage.domain.model.TtsAudio;
import FST.MST_RSI.PFA.voicemessage.domain.port.TextToSpeechPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ExecuteNotificationWorkflowUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExecuteNotificationWorkflowUseCase.class);
    private static final String DEFAULT_EMAIL_TEMPLATE = "DEFAULT-EMAIL-ALERT";

    private static final String EMAIL_PROVIDER = "smtp";
    private static final String SMS_PROVIDER = "kafka";
    private static final String VOIP_PROVIDER = "voip";

    private final EmailNotificationPort emailNotificationPort;
    private final SmsNotificationPort smsNotificationPort;
    private final NotificationRepositoryPort notificationRepositoryPort;
    private final NotificationTemplateJpaRepository templateRepository;
    private final EmailMessageComposer emailMessageComposer;
    private final SmsKafkaPayloadBuilder smsKafkaPayloadBuilder;
    private final PersonRepository personRepository;
    private final AlertRepositoryPort alertRepositoryPort;
    private final ScheduleResolutionCheckUseCase scheduleResolutionCheckUseCase;
    private final VoipNotificationProperties voipNotificationProperties;
    private final VoipMessageComposer voipMessageComposer;
    private final TextToSpeechPort textToSpeechPort;
    private final ObjectProvider<VoiceCallPort> voiceCallPortProvider;
    private final AuditRecorder auditRecorder;

    public ExecuteNotificationWorkflowUseCase(
            EmailNotificationPort emailNotificationPort,
            SmsNotificationPort smsNotificationPort,
            NotificationRepositoryPort notificationRepositoryPort,
            NotificationTemplateJpaRepository templateRepository,
            EmailMessageComposer emailMessageComposer,
            SmsKafkaPayloadBuilder smsKafkaPayloadBuilder,
            PersonRepository personRepository,
            AlertRepositoryPort alertRepositoryPort,
            ScheduleResolutionCheckUseCase scheduleResolutionCheckUseCase,
            VoipNotificationProperties voipNotificationProperties,
            VoipMessageComposer voipMessageComposer,
            TextToSpeechPort textToSpeechPort,
            ObjectProvider<VoiceCallPort> voiceCallPortProvider,
            AuditRecorder auditRecorder
    ) {
        this.emailNotificationPort = emailNotificationPort;
        this.smsNotificationPort = smsNotificationPort;
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.templateRepository = templateRepository;
        this.emailMessageComposer = emailMessageComposer;
        this.smsKafkaPayloadBuilder = smsKafkaPayloadBuilder;
        this.personRepository = personRepository;
        this.alertRepositoryPort = alertRepositoryPort;
        this.scheduleResolutionCheckUseCase = scheduleResolutionCheckUseCase;
        this.voipNotificationProperties = voipNotificationProperties;
        this.voipMessageComposer = voipMessageComposer;
        this.textToSpeechPort = textToSpeechPort;
        this.voiceCallPortProvider = voiceCallPortProvider;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public NotificationWorkflowResult execute(NotificationWorkflowCommand command) {
        Alert alert = command.alert();
        RoutingDecision routingDecision = command.routingDecision();
        BusinessDecision businessDecision = command.businessDecision();

        if (businessDecision.humanValidationRequired()
                || "AWAITING_HUMAN_VALIDATION".equals(routingDecision.routingStatus())) {
            return NotificationWorkflowResult.skipped("HUMAN_VALIDATION_REQUIRED");
        }

        if (!"STARTED".equals(routingDecision.routingStatus())) {
            if ("NO_PERSON".equals(routingDecision.routingStatus())) {
                alert.markNotificationFailed();
                alertRepositoryPort.save(alert);
            }
            return NotificationWorkflowResult.skipped(routingDecision.routingStatus());
        }

        NotificationChannel channel = routingDecision.currentStep() == null
                ? null
                : NotificationChannel.fromRoutingChannel(routingDecision.currentStep().channel());

        if (channel == null) {
            return NotificationWorkflowResult.skipped("NO_CHANNEL");
        }

        alert.markNotificationInProgress();
        alertRepositoryPort.save(alert);

        return switch (channel) {
            case EMAIL -> sendEmail(alert, command.classification(), routingDecision);
            case SMS -> sendSms(alert, command.classification(), routingDecision);
            case VOIP -> sendVoip(alert, command.classification(), routingDecision);
        };
    }

    private NotificationWorkflowResult sendEmail(
            Alert alert,
            ClassificationResult classification,
            RoutingDecision routingDecision
    ) {
        String recipientEmail = routingDecision.selectedPersonEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            alert.markNotificationFailed();
            alertRepositoryPort.save(alert);
            return NotificationWorkflowResult.skipped("NO_RECIPIENT_EMAIL");
        }

        NotificationTemplateEntity template = templateRepository.findByCodeAndActiveTrue(DEFAULT_EMAIL_TEMPLATE)
                .orElse(null);
        String subject = template != null
                ? emailMessageComposer.renderSubject(template, alert, classification)
                : "[AlertOps] " + alert.getTitle();
        String body = template != null
                ? emailMessageComposer.renderBody(template, alert, classification, routingDecision.selectedPersonName())
                : buildFallbackBody(alert, classification, routingDecision.selectedPersonName());

        NotificationRecord notification = notificationRepositoryPort.createPending(
                alert.getId().value(),
                routingDecision.routingExecutionId(),
                NotificationType.EMAIL,
                routingDecision.selectedPersonId(),
                recipientEmail
        );

        NotificationDeliveryRequest deliveryRequest = new NotificationDeliveryRequest(
                alert.getId().value(),
                routingDecision.routingExecutionId(),
                recipientEmail,
                routingDecision.selectedPersonName(),
                routingDecision.selectedPersonId(),
                subject,
                body,
                alert.getId().value().toString()
        );

        NotificationDeliveryResult deliveryResult = emailNotificationPort.send(deliveryRequest);
        notificationRepositoryPort.recordAttempt(
                notification.id(),
                1,
                EMAIL_PROVIDER,
                deliveryResult.success() ? NotificationStatus.SENT : NotificationStatus.FAILED,
                deliveryResult.providerMessageId(),
                deliveryResult.errorMessage()
        );

        if (deliveryResult.success()) {
            notificationRepositoryPort.updateStatus(notification.id(), NotificationStatus.SENT);
            alert.markNotificationSent();
            alertRepositoryPort.save(alert);
            log.info("Email notification sent for alert {}", alert.getId().value());
            scheduleResolutionCheck(alert);
            auditNotification(alert, routingDecision, notification.id(), "EMAIL", "EMAIL_SENT", NotificationStatus.SENT);
            return NotificationWorkflowResult.emailSent(notification.id());
        }

        notificationRepositoryPort.updateStatus(notification.id(), NotificationStatus.FAILED);
        alert.markNotificationFailed();
        alertRepositoryPort.save(alert);
        log.warn("Email notification failed for alert {}: {}", alert.getId().value(), deliveryResult.errorMessage());
        auditNotification(alert, routingDecision, notification.id(), "EMAIL", "EMAIL_FAILED", NotificationStatus.FAILED);
        return NotificationWorkflowResult.emailFailed(notification.id(), deliveryResult.errorMessage());
    }

    private NotificationWorkflowResult sendSms(
            Alert alert,
            ClassificationResult classification,
            RoutingDecision routingDecision
    ) {
        UUID personId = routingDecision.selectedPersonId();
        if (personId == null) {
            alert.markNotificationFailed();
            alertRepositoryPort.save(alert);
            return NotificationWorkflowResult.skipped("NO_RECIPIENT_PERSON");
        }

        PersonEntity person = personRepository.findById(personId).orElse(null);
        String phone = person != null ? person.getPhone() : null;
        if (phone == null || phone.isBlank()) {
            alert.markNotificationFailed();
            alertRepositoryPort.save(alert);
            return NotificationWorkflowResult.skipped("NO_RECIPIENT_PHONE");
        }

        SmsNotificationRequest smsRequest = smsKafkaPayloadBuilder.fromAlertContext(
                alert,
                classification,
                routingDecision.routingExecutionId(),
                personId,
                phone,
                routingDecision.selectedPersonName()
        );

        NotificationRecord notification = notificationRepositoryPort.createPending(
                alert.getId().value(),
                routingDecision.routingExecutionId(),
                NotificationType.SMS,
                personId,
                phone
        );

        NotificationDeliveryResult deliveryResult = smsNotificationPort.send(smsRequest);
        notificationRepositoryPort.recordAttempt(
                notification.id(),
                1,
                SMS_PROVIDER,
                deliveryResult.success() ? NotificationStatus.SENT : NotificationStatus.FAILED,
                deliveryResult.providerMessageId(),
                deliveryResult.errorMessage()
        );

        if (deliveryResult.success()) {
            notificationRepositoryPort.updateStatus(notification.id(), NotificationStatus.SENT);
            alert.markNotificationSent();
            alertRepositoryPort.save(alert);
            log.info("SMS Kafka notification published for alert {}", alert.getId().value());
            scheduleResolutionCheck(alert);
            auditNotification(alert, routingDecision, notification.id(), "SMS", "SMS_SENT", NotificationStatus.SENT);
            return NotificationWorkflowResult.smsSent(notification.id());
        }

        notificationRepositoryPort.updateStatus(notification.id(), NotificationStatus.FAILED);
        alert.markNotificationFailed();
        alertRepositoryPort.save(alert);
        auditNotification(alert, routingDecision, notification.id(), "SMS", "SMS_FAILED", NotificationStatus.FAILED);
        return NotificationWorkflowResult.smsFailed(notification.id(), deliveryResult.errorMessage());
    }

    private NotificationWorkflowResult sendVoip(
            Alert alert,
            ClassificationResult classification,
            RoutingDecision routingDecision
    ) {
        if (!voipNotificationProperties.isEnabled()) {
            return deferChannel(alert, routingDecision, NotificationChannel.VOIP);
        }

        VoiceCallPort voiceCallPort = voiceCallPortProvider.getIfAvailable();
        if (voiceCallPort == null) {
            return deferChannel(alert, routingDecision, NotificationChannel.VOIP);
        }

        UUID personId = routingDecision.selectedPersonId();
        if (personId == null) {
            alert.markNotificationFailed();
            alertRepositoryPort.save(alert);
            return NotificationWorkflowResult.skipped("NO_RECIPIENT_PERSON");
        }

        PersonEntity person = personRepository.findById(personId).orElse(null);
        String phone = person != null ? person.getPhone() : null;
        if (phone == null || phone.isBlank()) {
            alert.markNotificationFailed();
            alertRepositoryPort.save(alert);
            return NotificationWorkflowResult.skipped("NO_RECIPIENT_PHONE");
        }

        String message = voipMessageComposer.compose(alert, classification, routingDecision.selectedPersonName());
        TtsAudio audio = textToSpeechPort.synthesize(message).orElse(null);

        NotificationRecord notification = notificationRepositoryPort.createPending(
                alert.getId().value(),
                routingDecision.routingExecutionId(),
                NotificationType.VOIP,
                personId,
                phone
        );

        VoiceCallRequest callRequest = new VoiceCallRequest(
                alert.getId().value(),
                routingDecision.routingExecutionId(),
                personId,
                phone,
                routingDecision.selectedPersonName(),
                message,
                audio != null ? audio.content() : null,
                audio != null ? audio.contentType() : null,
                alert.getId().value().toString()
        );

        NotificationDeliveryResult deliveryResult = voiceCallPort.call(callRequest);
        notificationRepositoryPort.recordAttempt(
                notification.id(),
                1,
                VOIP_PROVIDER,
                deliveryResult.success() ? NotificationStatus.SENT : NotificationStatus.FAILED,
                deliveryResult.providerMessageId(),
                deliveryResult.errorMessage()
        );

        if (deliveryResult.success()) {
            notificationRepositoryPort.updateStatus(notification.id(), NotificationStatus.SENT);
            alert.markNotificationSent();
            alertRepositoryPort.save(alert);
            log.info("VoIP call initiated for alert {} to extension {}", alert.getId().value(), phone);
            scheduleResolutionCheck(alert);
            auditNotification(alert, routingDecision, notification.id(), "VOIP", "VOIP_SENT", NotificationStatus.SENT);
            return NotificationWorkflowResult.voipSent(notification.id());
        }

        notificationRepositoryPort.updateStatus(notification.id(), NotificationStatus.FAILED);
        alert.markNotificationFailed();
        alertRepositoryPort.save(alert);
        auditNotification(alert, routingDecision, notification.id(), "VOIP", "VOIP_FAILED", NotificationStatus.FAILED);
        return NotificationWorkflowResult.voipFailed(notification.id(), deliveryResult.errorMessage());
    }

    private NotificationWorkflowResult deferChannel(
            Alert alert,
            RoutingDecision routingDecision,
            NotificationChannel channel
    ) {
        NotificationType type = NotificationType.VOIP;
        String destination = routingDecision.selectedPersonEmail() != null
                ? routingDecision.selectedPersonEmail()
                : personRepository.findById(routingDecision.selectedPersonId())
                        .map(PersonEntity::getPhone)
                        .orElse("pending");

        NotificationRecord notification = notificationRepositoryPort.createPending(
                alert.getId().value(),
                routingDecision.routingExecutionId(),
                type,
                routingDecision.selectedPersonId(),
                destination
        );
        notificationRepositoryPort.updateStatus(notification.id(), NotificationStatus.DEFERRED);
        alertRepositoryPort.save(alert);
        scheduleResolutionCheck(alert);
        auditNotification(alert, routingDecision, notification.id(), channel.name(), "DEFERRED_" + channel.name(), NotificationStatus.DEFERRED);
        return NotificationWorkflowResult.deferred(channel.name(), notification.id());
    }

    private void auditNotification(
            Alert alert,
            RoutingDecision routingDecision,
            UUID notificationId,
            String channel,
            String outcome,
            NotificationStatus status
    ) {
        auditRecorder.record(new AuditRecord(
                AuditAction.NOTIFICATION_ATTEMPTED,
                alert.getId().value(),
                null,
                routingDecision.routingExecutionId(),
                notificationId,
                routingDecision.selectedPersonId(),
                "Notification",
                notificationId,
                "Notification " + channel + ": outcome=" + outcome + ", status=" + status
                        + " (envoi technique, pas accusé de prise en charge)",
                AuditRecorder.correlationId(alert.getId().value()),
                null,
                List.of(
                        new AuditRecord.AuditDetail("channel", null, channel),
                        new AuditRecord.AuditDetail("recipient", null, routingDecision.selectedPersonName())
                )
        ));
    }

    private void scheduleResolutionCheck(Alert alert) {
        scheduleResolutionCheckUseCase.execute(alert);
    }

    private static String buildFallbackBody(Alert alert, ClassificationResult classification, String recipientName) {
        return """
                Bonjour %s,

                Une alerte nécessite votre attention.

                Problème : %s
                Titre : %s
                Sévérité : %s

                Cet e-mail confirme uniquement l'envoi technique de la notification.
                """.formatted(
                recipientName != null ? recipientName : "collègue",
                alert.getExternalProblemId(),
                alert.getTitle(),
                alert.getSeverity()
        );
    }

    public record NotificationWorkflowCommand(
            Alert alert,
            ClassificationResult classification,
            BusinessDecision businessDecision,
            RoutingDecision routingDecision
    ) {
    }

    public record NotificationWorkflowResult(
            String outcome,
            UUID notificationId,
            NotificationStatus status,
            String detail
    ) {
        public static NotificationWorkflowResult skipped(String reason) {
            return new NotificationWorkflowResult("SKIPPED", null, NotificationStatus.SKIPPED, reason);
        }

        public static NotificationWorkflowResult emailSent(UUID notificationId) {
            return new NotificationWorkflowResult("EMAIL_SENT", notificationId, NotificationStatus.SENT, null);
        }

        public static NotificationWorkflowResult emailFailed(UUID notificationId, String detail) {
            return new NotificationWorkflowResult("EMAIL_FAILED", notificationId, NotificationStatus.FAILED, detail);
        }

        public static NotificationWorkflowResult smsSent(UUID notificationId) {
            return new NotificationWorkflowResult("SMS_SENT", notificationId, NotificationStatus.SENT, null);
        }

        public static NotificationWorkflowResult smsFailed(UUID notificationId, String detail) {
            return new NotificationWorkflowResult("SMS_FAILED", notificationId, NotificationStatus.FAILED, detail);
        }

        public static NotificationWorkflowResult deferred(String channel, UUID notificationId) {
            return new NotificationWorkflowResult("DEFERRED_" + channel, notificationId, NotificationStatus.DEFERRED, null);
        }

        public static NotificationWorkflowResult voipSent(UUID notificationId) {
            return new NotificationWorkflowResult("VOIP_SENT", notificationId, NotificationStatus.SENT, null);
        }

        public static NotificationWorkflowResult voipFailed(UUID notificationId, String detail) {
            return new NotificationWorkflowResult("VOIP_FAILED", notificationId, NotificationStatus.FAILED, detail);
        }
    }
}
