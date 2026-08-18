package FST.MST_RSI.PFA.audit.application.usecase;

import FST.MST_RSI.PFA.audit.application.dto.AuditLogDto;
import FST.MST_RSI.PFA.audit.application.dto.AuditTimelineEntryDto;
import FST.MST_RSI.PFA.audit.application.dto.SystemEventDto;
import FST.MST_RSI.PFA.audit.application.mapper.AuditMapper;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.AuditLogDetailJpaRepository;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.AuditLogEntity;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.AuditLogJpaRepository;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.SystemEventEntity;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.SystemEventJpaRepository;
import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class GetAlertAuditTimelineUseCase {

    private final AuditLogJpaRepository auditLogJpaRepository;
    private final AuditLogDetailJpaRepository auditLogDetailJpaRepository;
    private final SystemEventJpaRepository systemEventJpaRepository;

    public GetAlertAuditTimelineUseCase(
            AuditLogJpaRepository auditLogJpaRepository,
            AuditLogDetailJpaRepository auditLogDetailJpaRepository,
            SystemEventJpaRepository systemEventJpaRepository
    ) {
        this.auditLogJpaRepository = auditLogJpaRepository;
        this.auditLogDetailJpaRepository = auditLogDetailJpaRepository;
        this.systemEventJpaRepository = systemEventJpaRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditTimelineEntryDto> execute(String alertId) {
        UUID alertUuid = parseAlertId(alertId);
        List<AuditTimelineEntryDto> entries = new ArrayList<>();

        for (AuditLogEntity audit : auditLogJpaRepository.findByAlertIdOrderByCreatedAtAsc(alertUuid)) {
            AuditLogDto dto = AuditMapper.toDto(
                    audit,
                    auditLogDetailJpaRepository.findByAuditLogIdOrderByFieldNameAsc(audit.getId())
            );
            entries.add(new AuditTimelineEntryDto(
                    "AUDIT",
                    audit.getCreatedAt(),
                    audit.getAction(),
                    audit.getDescription(),
                    null,
                    null,
                    audit.getCorrelationId(),
                    dto,
                    null
            ));
        }

        for (SystemEventEntity event : systemEventJpaRepository.findByAlertIdOrderByCreatedAtAsc(alertUuid)) {
            SystemEventDto dto = AuditMapper.toDto(event);
            entries.add(new AuditTimelineEntryDto(
                    "SYSTEM",
                    event.getCreatedAt(),
                    event.getEventType(),
                    event.getMessage(),
                    event.getSeverity(),
                    event.getSourceModule(),
                    event.getCorrelationId(),
                    null,
                    dto
            ));
        }

        entries.sort(Comparator.comparing(AuditTimelineEntryDto::occurredAt));
        return entries;
    }

    private static UUID parseAlertId(String alertId) {
        try {
            return UUID.fromString(alertId);
        } catch (IllegalArgumentException ex) {
            throw new ResourceNotFoundException("Alert not found: " + alertId);
        }
    }
}
