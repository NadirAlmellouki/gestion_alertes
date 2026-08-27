package FST.MST_RSI.PFA.audit.application.usecase;

import FST.MST_RSI.PFA.audit.domain.model.AuditRecord;
import FST.MST_RSI.PFA.audit.domain.model.SystemEventRecord;
import FST.MST_RSI.PFA.audit.domain.port.AuditRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Use case responsible for persisting audit records.
 *
 * <p><strong>Propagation strategy</strong>: {@code REQUIRED} (joins the caller's transaction).
 * This guarantees that referenced entities (e.g. {@code alert_llm_analysis}) committed by
 * the parent transaction are visible when the FK constraint is checked, avoiding the
 * {@code audit_log_llm_analysis_id_fkey} violation that occurred with {@code REQUIRES_NEW}.
 *
 * <p>For callers that need the audit to run <em>after</em> the parent transaction commits
 * (e.g. to avoid FK issues when the parent entity is not yet flushed), use
 * {@link #executeAfterCommit(AuditRecord)} instead.
 */
@Service
public class RecordAuditEventUseCase {

    private final AuditRepositoryPort auditRepositoryPort;

    public RecordAuditEventUseCase(AuditRepositoryPort auditRepositoryPort) {
        this.auditRepositoryPort = auditRepositoryPort;
    }

    /**
     * Persists an audit record within the <em>current</em> transaction (REQUIRED).
     * Use this when the audit record references entities that are already flushed/committed
     * in the current transaction.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public UUID execute(AuditRecord record) {
        return auditRepositoryPort.saveAudit(record);
    }

    /**
     * Registers the audit record to be persisted in a <em>new</em> transaction
     * immediately <strong>after</strong> the current transaction commits.
     * Use this when the referenced entity (e.g. {@code alert_llm_analysis}) is inserted
     * in the same parent transaction and must be fully committed before the audit FK resolves.
     */
    public void executeAfterCommit(AuditRecord record) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executeInNewTransaction(record);
                }
            });
        } else {
            // No active transaction — execute immediately
            executeInNewTransaction(record);
        }
    }

    /**
     * Internal helper: persists the record in a brand-new transaction.
     * Called after the parent transaction has committed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID executeInNewTransaction(AuditRecord record) {
        return auditRepositoryPort.saveAudit(record);
    }

    /**
     * Persists a system event record within the current transaction (REQUIRED).
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public UUID executeSystemEvent(SystemEventRecord record) {
        return auditRepositoryPort.saveSystemEvent(record);
    }
}
