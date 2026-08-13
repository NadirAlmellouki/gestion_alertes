package FST.MST_RSI.PFA.routingengine.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoutingExecutionRepository extends JpaRepository<RoutingExecutionEntity, UUID> {
}
