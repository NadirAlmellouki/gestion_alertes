package FST.MST_RSI.PFA.audit.application.usecase;

import FST.MST_RSI.PFA.audit.domain.model.AuditRecord;
import FST.MST_RSI.PFA.audit.domain.model.SystemEventRecord;
import FST.MST_RSI.PFA.audit.domain.port.AuditRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RecordAuditEventUseCase {

    private final AuditRepositoryPort auditRepositoryPort;

    public RecordAuditEventUseCase(AuditRepositoryPort auditRepositoryPort) {
        this.auditRepositoryPort = auditRepositoryPort;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID execute(AuditRecord record) {
        return auditRepositoryPort.saveAudit(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID executeSystemEvent(SystemEventRecord record) {
        return auditRepositoryPort.saveSystemEvent(record);
    }
}
