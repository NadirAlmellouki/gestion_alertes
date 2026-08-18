package FST.MST_RSI.PFA.audit.domain.port;

import FST.MST_RSI.PFA.audit.domain.model.AuditRecord;
import FST.MST_RSI.PFA.audit.domain.model.SystemEventRecord;

import java.util.UUID;

public interface AuditRepositoryPort {

    UUID saveAudit(AuditRecord record);

    UUID saveSystemEvent(SystemEventRecord record);
}
