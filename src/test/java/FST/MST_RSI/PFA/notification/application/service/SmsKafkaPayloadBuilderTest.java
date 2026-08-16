package FST.MST_RSI.PFA.notification.application.service;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationCategory;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationStatus;
import FST.MST_RSI.PFA.common.domain.vo.Confidence;
import FST.MST_RSI.PFA.notification.domain.model.SmsNotificationRequest;
import FST.MST_RSI.PFA.notification.infrastructure.config.SmsNotificationProperties;
import FST.MST_RSI.PFA.notification.infrastructure.sms.SmsKafkaEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SmsKafkaPayloadBuilderTest {

    private SmsKafkaPayloadBuilder builder;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        SmsNotificationProperties properties = new SmsNotificationProperties();
        properties.setAppName("alertops");
        properties.setEventKey("FLASH_NOTIFICATION_EVENT");
        properties.setEventId("KRAKEN_NOTIFICATION_SENDING");
        builder = new SmsKafkaPayloadBuilder(properties);
        objectMapper = new ObjectMapper();
    }

    @Test
    void buildsEnterpriseKafkaPayloadShape() throws Exception {
        UUID alertId = UUID.randomUUID();
        SmsNotificationRequest request = new SmsNotificationRequest(
                alertId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "1001",
                "Jane Doe",
                "PayCore",
                "CPU saturation",
                "CRITICAL",
                "P-123",
                "PayCore",
                SmsKafkaPayloadBuilder.sha256Hex(alertId.toString())
        );

        List<SmsKafkaEnvelope> payload = builder.build(request);
        String json = objectMapper.writeValueAsString(payload);

        assertThat(payload).hasSize(1);
        assertThat(payload.getFirst().appName()).isEqualTo("PayCore");
        assertThat(payload.getFirst().eventKey()).isEqualTo("FLASH_NOTIFICATION_EVENT");
        assertThat(payload.getFirst().eventId()).isEqualTo("KRAKEN_NOTIFICATION_SENDING");
        assertThat(payload.getFirst().eventType()).isEqualTo("NOTIFICATION");
        assertThat(payload.getFirst().notificationRequest().push().when().realtime()).isTrue();
        assertThat(payload.getFirst().notificationRequest().push().message().content()).contains("P-123");
        assertThat(json).contains("notificationRequest");
        assertThat(json).contains("correlationId");
    }
}
