package FST.MST_RSI.PFA.notification.application.dto;

import java.time.Instant;
import java.util.UUID;

public record VoiceCallSessionStatusDto(
        UUID sessionId,
        String outcome,
        boolean active,
        String supervisorChannelId,
        String adminChannelId,
        String supervisorExtension,
        Instant startedAt,
        Instant answeredAt,
        Instant endedAt,
        Integer hangupCause
) {
}
