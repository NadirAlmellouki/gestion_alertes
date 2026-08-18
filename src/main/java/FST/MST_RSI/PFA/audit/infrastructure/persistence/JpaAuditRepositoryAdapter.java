package FST.MST_RSI.PFA.audit.infrastructure.persistence;

import FST.MST_RSI.PFA.audit.domain.model.AuditRecord;
import FST.MST_RSI.PFA.audit.domain.model.SystemEventRecord;
import FST.MST_RSI.PFA.audit.domain.port.AuditRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class JpaAuditRepositoryAdapter implements AuditRepositoryPort {

    private final AuditLogJpaRepository auditLogJpaRepository;
    private final AuditLogDetailJpaRepository auditLogDetailJpaRepository;
    private final SystemEventJpaRepository systemEventJpaRepository;

    public JpaAuditRepositoryAdapter(
            AuditLogJpaRepository auditLogJpaRepository,
            AuditLogDetailJpaRepository auditLogDetailJpaRepository,
            SystemEventJpaRepository systemEventJpaRepository
    ) {
        this.auditLogJpaRepository = auditLogJpaRepository;
        this.auditLogDetailJpaRepository = auditLogDetailJpaRepository;
        this.systemEventJpaRepository = systemEventJpaRepository;
    }

    @Override
    public UUID saveAudit(AuditRecord record) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        auditLogJpaRepository.save(AuditLogEntity.create(
                id,
                record.actorPersonId(),
                record.alertId(),
                record.llmAnalysisId(),
                record.routingExecutionId(),
                record.notificationId(),
                record.action(),
                record.entityName(),
                record.entityId(),
                record.description(),
                record.correlationId(),
                record.ipAddress(),
                now
        ));
        for (AuditRecord.AuditDetail detail : record.details()) {
            auditLogDetailJpaRepository.save(AuditLogDetailEntity.create(
                    UUID.randomUUID(),
                    id,
                    detail.fieldName(),
                    detail.oldValue(),
                    detail.newValue()
            ));
        }
        return id;
    }

    @Override
    public UUID saveSystemEvent(SystemEventRecord record) {
        UUID id = UUID.randomUUID();
        systemEventJpaRepository.save(SystemEventEntity.create(
                id,
                record.alertId(),
                record.llmAnalysisId(),
                record.sourceModule(),
                record.severity(),
                record.eventType(),
                record.message(),
                record.correlationId(),
                Instant.now()
        ));
        return id;
    }
}
