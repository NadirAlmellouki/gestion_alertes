package FST.MST_RSI.PFA.routingengine.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoutingHistoryRepository extends JpaRepository<RoutingHistoryEntity, UUID> {

    List<RoutingHistoryEntity> findByRoutingExecutionIdOrderByActionTimeAsc(UUID routingExecutionId);
}
