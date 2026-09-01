package FST.MST_RSI.PFA.dashboard.application.dto;

import java.time.Instant;

public record VoipCallDto(
        String notificationId,
        String alertId,
        String personName,
        String destination,
        String status,
        Instant createdAt,
        Integer escalationStep,
        String outcome,
        Integer durationSeconds,
        Boolean liveMode,
        String hangupSource,
        String failureReason
) {
    public VoipCallDto(
            String notificationId,
            String alertId,
            String personName,
            String destination,
            String status,
            Instant createdAt,
            Integer escalationStep
    ) {
        this(
                notificationId,
                alertId,
                personName,
                destination,
                status,
                createdAt,
                escalationStep,
                status,
                null,
                false,
                null,
                null
        );
    }
}
