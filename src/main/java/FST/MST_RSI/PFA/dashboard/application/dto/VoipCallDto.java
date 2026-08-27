package FST.MST_RSI.PFA.dashboard.application.dto;

import java.time.Instant;

public record VoipCallDto(
        String notificationId,
        String alertId,
        String personName,
        String destination,
        String status,
        Instant createdAt,
        Integer escalationStep
) {
}
