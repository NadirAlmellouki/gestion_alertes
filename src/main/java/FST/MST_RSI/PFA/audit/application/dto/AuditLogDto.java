package FST.MST_RSI.PFA.audit.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        UUID alertId,
        UUID llmAnalysisId,
        UUID routingExecutionId,
        UUID notificationId,
        String action,
        String entityName,
        UUID entityId,
        String description,
        String correlationId,
        Instant createdAt,
        List<AuditLogDetailDto> details
) {
    public record AuditLogDetailDto(String fieldName, String oldValue, String newValue) {
    }
}
