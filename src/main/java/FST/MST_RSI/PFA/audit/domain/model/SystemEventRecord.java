package FST.MST_RSI.PFA.audit.domain.model;

import java.util.UUID;

public record SystemEventRecord(
        UUID alertId,
        UUID llmAnalysisId,
        String sourceModule,
        String severity,
        String eventType,
        String message,
        String correlationId
) {
}
