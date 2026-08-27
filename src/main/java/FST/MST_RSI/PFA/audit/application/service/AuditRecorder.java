package FST.MST_RSI.PFA.audit.application.service;

import FST.MST_RSI.PFA.audit.application.usecase.RecordAuditEventUseCase;
import FST.MST_RSI.PFA.audit.domain.model.AuditRecord;
import FST.MST_RSI.PFA.audit.domain.model.SystemEventRecord;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditRecorder {

    private final RecordAuditEventUseCase recordAuditEventUseCase;

    public AuditRecorder(RecordAuditEventUseCase recordAuditEventUseCase) {
        this.recordAuditEventUseCase = recordAuditEventUseCase;
    }

    /**
     * Records the audit event within the current transaction (REQUIRED propagation).
     * Safe for use when referenced entities are already in the current tx.
     */
    public UUID record(AuditRecord record) {
        return recordAuditEventUseCase.execute(record);
    }

    /**
     * Defers the audit record to be persisted in a new transaction
     * <strong>after</strong> the current transaction commits.
     * Use this when referenced FK entities (e.g. notification, llm_analysis)
     * are being committed in the same parent transaction.
     */
    public void recordAfterCommit(AuditRecord record) {
        recordAuditEventUseCase.executeAfterCommit(record);
    }

    public UUID recordSystemEvent(SystemEventRecord record) {
        return recordAuditEventUseCase.executeSystemEvent(record);
    }

    public static String correlationId(UUID alertId) {
        return alertId == null ? null : alertId.toString();
    }
}
