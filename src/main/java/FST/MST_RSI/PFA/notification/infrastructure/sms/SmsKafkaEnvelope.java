package FST.MST_RSI.PFA.notification.infrastructure.sms;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SmsKafkaEnvelope(
        @JsonProperty("APP_NAME") String appName,
        @JsonProperty("eventKey") String eventKey,
        @JsonProperty("correlationId") String correlationId,
        @JsonProperty("eventId") String eventId,
        @JsonProperty("eventType") String eventType,
        @JsonProperty("notificationRequest") NotificationRequest notificationRequest
) {
    public record NotificationRequest(
            Client client,
            String correlationId,
            Push push
    ) {
    }

    public record Client(
            @JsonProperty("accountNumber") String accountNumber
    ) {
    }

    public record Push(
            When when,
            Message message,
            int priority,
            boolean timeline
    ) {
    }

    public record When(
            boolean realtime
    ) {
    }

    public record Message(
            String title,
            String content
    ) {
    }
}
