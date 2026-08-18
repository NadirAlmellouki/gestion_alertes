package FST.MST_RSI.PFA.audit.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SystemEventDto(
        UUID id,
        UUID alertId,
        String sourceModule,
        String severity,
        String eventType,
        String message,
        String correlationId,
        Instant createdAt
) {
}
