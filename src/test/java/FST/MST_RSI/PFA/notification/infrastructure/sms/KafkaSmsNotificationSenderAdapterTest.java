package FST.MST_RSI.PFA.notification.infrastructure.sms;

import FST.MST_RSI.PFA.notification.application.service.SmsKafkaPayloadBuilder;
import FST.MST_RSI.PFA.notification.domain.model.SmsNotificationRequest;
import FST.MST_RSI.PFA.notification.infrastructure.config.SmsNotificationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaSmsNotificationSenderAdapterTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaSmsNotificationSenderAdapter adapter;

    @BeforeEach
    void setUp() {
        SmsNotificationProperties properties = new SmsNotificationProperties();
        properties.setEnabled(true);
        properties.getKafka().setTopic("alertops-sms-notifications");
        adapter = new KafkaSmsNotificationSenderAdapter(
                kafkaTemplate,
                properties,
                new SmsKafkaPayloadBuilder(properties),
                new ObjectMapper()
        );
    }

    @Test
    void publishesJsonPayloadToKafkaTopic() throws Exception {
        UUID alertId = UUID.randomUUID();
        String correlationId = SmsKafkaPayloadBuilder.sha256Hex(alertId.toString());
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
                correlationId
        );

        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("alertops-sms-notifications", 0),
                0,
                42,
                System.currentTimeMillis(),
                0,
                0
        );
        SendResult<String, String> sendResult = new SendResult<>(
                new ProducerRecord<>("alertops-sms-notifications", correlationId, "{}"),
                metadata
        );
        when(kafkaTemplate.send(eq("alertops-sms-notifications"), eq(correlationId), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        var result = adapter.send(request);

        assertThat(result.success()).isTrue();
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("alertops-sms-notifications"), eq(correlationId), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).contains("KRAKEN_NOTIFICATION_SENDING");
    }
}
