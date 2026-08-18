package FST.MST_RSI.PFA.audit.application.usecase;

import FST.MST_RSI.PFA.audit.application.dto.AuditLogDto;
import FST.MST_RSI.PFA.audit.application.mapper.AuditMapper;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.AuditLogDetailJpaRepository;
import FST.MST_RSI.PFA.audit.infrastructure.persistence.AuditLogJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ListAuditLogsUseCase {

    private final AuditLogJpaRepository auditLogJpaRepository;
    private final AuditLogDetailJpaRepository auditLogDetailJpaRepository;

    public ListAuditLogsUseCase(
            AuditLogJpaRepository auditLogJpaRepository,
            AuditLogDetailJpaRepository auditLogDetailJpaRepository
    ) {
        this.auditLogJpaRepository = auditLogJpaRepository;
        this.auditLogDetailJpaRepository = auditLogDetailJpaRepository;
    }

    @Transactional(readOnly = true)
    public List<AuditLogDto> execute(
            UUID alertId,
            String action,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        Page<AuditLogDto> result = auditLogJpaRepository.search(
                alertId,
                blankToNull(action),
                from,
                to,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)))
        ).map(entity -> AuditMapper.toDto(
                entity,
                auditLogDetailJpaRepository.findByAuditLogIdOrderByFieldNameAsc(entity.getId())
        ));
        return result.getContent();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
