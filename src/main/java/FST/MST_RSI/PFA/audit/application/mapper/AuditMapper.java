package FST.MST_RSI.PFA.audit.application.mapper;

import FST.MST_RSI.PFA.audit.application.dto.AuditLogDto;
import FST.MST_RSI.PFA.audit.application.dto.SystemEventDto;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.AuditLogDetailEntity;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.AuditLogEntity;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.SystemEventEntity;

import java.util.List;

public final class AuditMapper {

    private AuditMapper() {
    }

    public static AuditLogDto toDto(AuditLogEntity entity, List<AuditLogDetailEntity> details) {
        return new AuditLogDto(
                entity.getId(),
                entity.getAlertId(),
                entity.getLlmAnalysisId(),
                entity.getRoutingExecutionId(),
                entity.getNotificationId(),
                entity.getAction(),
                entity.getEntityName(),
                entity.getEntityId(),
                entity.getDescription(),
                entity.getCorrelationId(),
                entity.getCreatedAt(),
                details.stream()
                        .map(d -> new AuditLogDto.AuditLogDetailDto(d.getFieldName(), d.getOldValue(), d.getNewValue()))
                        .toList()
        );
    }

    public static SystemEventDto toDto(SystemEventEntity entity) {
        return new SystemEventDto(
                entity.getId(),
                entity.getAlertId(),
                entity.getSourceModule(),
                entity.getSeverity(),
                entity.getEventType(),
                entity.getMessage(),
                entity.getCorrelationId(),
                entity.getCreatedAt()
        );
    }
}
