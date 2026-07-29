package FST.MST_RSI.PFA.audit.infrastructure.persistence;

import FST.MST_RSI.PFA.audit.domain.model.SystemLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<SystemLogEntry, Long> {
}
