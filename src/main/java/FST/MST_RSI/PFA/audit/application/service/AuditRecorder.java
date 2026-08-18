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

    public UUID record(AuditRecord record) {
        return recordAuditEventUseCase.execute(record);
    }

    public UUID recordSystemEvent(SystemEventRecord record) {
        return recordAuditEventUseCase.executeSystemEvent(record);
    }

    public static String correlationId(UUID alertId) {
        return alertId == null ? null : alertId.toString();
    }
}
