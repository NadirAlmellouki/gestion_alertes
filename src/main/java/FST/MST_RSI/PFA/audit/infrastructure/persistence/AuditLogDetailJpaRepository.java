package FST.MST_RSI.PFA.audit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogDetailJpaRepository extends JpaRepository<AuditLogDetailEntity, UUID> {

    List<AuditLogDetailEntity> findByAuditLogIdOrderByFieldNameAsc(UUID auditLogId);
}
