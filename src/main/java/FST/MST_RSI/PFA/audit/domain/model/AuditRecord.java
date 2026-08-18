package FST.MST_RSI.PFA.audit.domain.model;

import java.util.List;
import java.util.UUID;

public record AuditRecord(
        String action,
        UUID alertId,
        UUID llmAnalysisId,
        UUID routingExecutionId,
        UUID notificationId,
        UUID actorPersonId,
        String entityName,
        UUID entityId,
        String description,
        String correlationId,
        String ipAddress,
        List<AuditDetail> details
) {
    public AuditRecord {
        details = details == null ? List.of() : List.copyOf(details);
    }

    public record AuditDetail(String fieldName, String oldValue, String newValue) {
    }
}
