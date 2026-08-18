package FST.MST_RSI.PFA.audit.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SystemEventJpaRepository extends JpaRepository<SystemEventEntity, UUID> {

    Page<SystemEventEntity> findByAlertIdOrderByCreatedAtDesc(UUID alertId, Pageable pageable);

    List<SystemEventEntity> findByAlertIdOrderByCreatedAtAsc(UUID alertId);
}
