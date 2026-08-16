package FST.MST_RSI.PFA.notification.infrastructure.sms;

import FST.MST_RSI.PFA.notification.application.service.SmsKafkaPayloadBuilder;
import FST.MST_RSI.PFA.notification.domain.model.NotificationDeliveryResult;
import FST.MST_RSI.PFA.notification.domain.model.SmsNotificationRequest;
import FST.MST_RSI.PFA.notification.domain.port.SmsNotificationPort;
import FST.MST_RSI.PFA.notification.infrastructure.config.SmsNotificationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "app.notification.sms.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaSmsNotificationSenderAdapter implements SmsNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaSmsNotificationSenderAdapter.class);
    private static final String PROVIDER = "kafka";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SmsNotificationProperties properties;
    private final SmsKafkaPayloadBuilder payloadBuilder;
    private final ObjectMapper objectMapper;

    public KafkaSmsNotificationSenderAdapter(
            KafkaTemplate<String, String> kafkaTemplate,
            SmsNotificationProperties properties,
            SmsKafkaPayloadBuilder payloadBuilder,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.payloadBuilder = payloadBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public NotificationDeliveryResult send(SmsNotificationRequest request) {
        if (!properties.isEnabled()) {
            return NotificationDeliveryResult.failed("SMS Kafka producer is disabled");
        }
        try {
            List<SmsKafkaEnvelope> payload = payloadBuilder.build(request);
            String json = objectMapper.writeValueAsString(payload);
            String topic = properties.getKafka().getTopic();
            SendResult<String, String> result = kafkaTemplate.send(topic, request.correlationId(), json)
                    .get(10, TimeUnit.SECONDS);
            String offset = result.getRecordMetadata().offset() + "@" + result.getRecordMetadata().partition();
            log.info("SMS Kafka message published for alert {} to topic {}", request.alertId(), topic);
            return NotificationDeliveryResult.sent(offset);
        } catch (Exception ex) {
            log.error("Failed to publish SMS Kafka message for alert {}", request.alertId(), ex);
            return NotificationDeliveryResult.failed(ex.getMessage());
        }
    }

    public String providerName() {
        return PROVIDER;
    }
}
