package FST.MST_RSI.PFA.notification.application.service;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.notification.domain.model.SmsNotificationRequest;
import FST.MST_RSI.PFA.notification.infrastructure.config.SmsNotificationProperties;
import FST.MST_RSI.PFA.notification.infrastructure.sms.SmsKafkaEnvelope;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class SmsKafkaPayloadBuilder {

    private final SmsNotificationProperties properties;

    public SmsKafkaPayloadBuilder(SmsNotificationProperties properties) {
        this.properties = properties;
    }

    public List<SmsKafkaEnvelope> build(SmsNotificationRequest request) {
        String correlationId = request.correlationId() != null
                ? request.correlationId()
                : sha256Hex(request.alertId().toString());

        String title = "[AlertOps] " + nullToEmpty(request.alertSeverity()) + " — "
                + nullToEmpty(request.alertTitle());
        String content = """
                Alerte %s
                Problème : %s
                Solution : %s
                Destinataire : %s
                """.formatted(
                nullToEmpty(request.alertSeverity()),
                nullToEmpty(request.problemId()),
                nullToEmpty(request.matchedSolution()),
                nullToEmpty(request.recipientName())
        ).trim();

        SmsKafkaEnvelope envelope = new SmsKafkaEnvelope(
                resolveAppName(request),
                properties.getEventKey(),
                correlationId,
                properties.getEventId(),
                "NOTIFICATION",
                new SmsKafkaEnvelope.NotificationRequest(
                        new SmsKafkaEnvelope.Client(""),
                        correlationId,
                        new SmsKafkaEnvelope.Push(
                                new SmsKafkaEnvelope.When(true),
                                new SmsKafkaEnvelope.Message(title, content),
                                0,
                                true
                        )
                )
        );
        return List.of(envelope);
    }

    public SmsNotificationRequest fromAlertContext(
            Alert alert,
            ClassificationResult classification,
            UUID routingExecutionId,
            UUID personId,
            String phone,
            String recipientName
    ) {
        String appName = alert.getApplicationName();
        if (appName == null || appName.isBlank()) {
            appName = classification != null ? classification.matchedSolution() : properties.getAppName();
        }
        return new SmsNotificationRequest(
                alert.getId().value(),
                routingExecutionId,
                personId,
                phone,
                recipientName,
                appName,
                alert.getTitle(),
                alert.getSeverity(),
                alert.getExternalProblemId(),
                classification != null ? classification.matchedSolution() : null,
                sha256Hex(alert.getId().value().toString())
        );
    }

    private String resolveAppName(SmsNotificationRequest request) {
        if (request.applicationName() != null && !request.applicationName().isBlank()) {
            return request.applicationName();
        }
        return properties.getAppName();
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
