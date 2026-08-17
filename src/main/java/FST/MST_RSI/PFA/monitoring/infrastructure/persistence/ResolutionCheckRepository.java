package FST.MST_RSI.PFA.monitoring.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResolutionCheckRepository extends JpaRepository<ResolutionCheckEntity, UUID> {

    @Query("""
            SELECT r FROM ResolutionCheckEntity r
            WHERE r.status = 'ACTIVE'
              AND r.nextCheckAt IS NOT NULL
              AND r.nextCheckAt <= :dueBefore
            """)
    List<ResolutionCheckEntity> findDueChecks(@Param("dueBefore") Instant dueBefore);

    Optional<ResolutionCheckEntity> findFirstByAlertIdAndStatus(UUID alertId, String status);
}
