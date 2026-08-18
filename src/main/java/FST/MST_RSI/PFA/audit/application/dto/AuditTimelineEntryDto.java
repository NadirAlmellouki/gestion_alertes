package FST.MST_RSI.PFA.audit.application.dto;

import java.time.Instant;

public record AuditTimelineEntryDto(
        String entryType,
        Instant occurredAt,
        String actionOrEventType,
        String description,
        String severity,
        String sourceModule,
        String correlationId,
        AuditLogDto auditLog,
        SystemEventDto systemEvent
) {
}
