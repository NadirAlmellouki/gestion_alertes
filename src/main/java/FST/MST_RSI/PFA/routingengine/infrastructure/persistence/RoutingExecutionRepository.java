package FST.MST_RSI.PFA.routingengine.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RoutingExecutionRepository extends JpaRepository<RoutingExecutionEntity, UUID> {

    @Query("""
            SELECT e FROM RoutingExecutionEntity e
            WHERE e.routingStatus = 'AWAITING_ESCALATION'
              AND e.nextEscalationAt IS NOT NULL
              AND e.nextEscalationAt <= :dueBefore
            """)
    List<RoutingExecutionEntity> findDueEscalations(@Param("dueBefore") Instant dueBefore);
}
