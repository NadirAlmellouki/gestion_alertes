package FST.MST_RSI.PFA.dashboard.application.dto;

import java.time.Instant;

public record AdminAvailabilityDto(
        String personId,
        String fullName,
        String email,
        String phone,
        boolean active,
        String availability,
        long totalNotifications,
        long successCount,
        long failedCount,
        long voipCalls,
        Instant lastContactAt,
        String lastVoipOutcome,
        String sipReachability
) {
}
