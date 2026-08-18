package FST.MST_RSI.PFA.audit.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {

    Page<AuditLogEntity> findByAlertIdOrderByCreatedAtDesc(UUID alertId, Pageable pageable);

    @Query("""
            SELECT a FROM AuditLogEntity a
            WHERE (:alertId IS NULL OR a.alertId = :alertId)
              AND (:action IS NULL OR a.action = :action)
              AND a.createdAt >= :from
              AND a.createdAt <= :to
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLogEntity> search(
            @Param("alertId") UUID alertId,
            @Param("action") String action,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    List<AuditLogEntity> findByAlertIdOrderByCreatedAtAsc(UUID alertId);
}
