package FST.MST_RSI.PFA.routingengine.infrastructure.persistence;

import FST.MST_RSI.PFA.routingengine.domain.model.PolicyOrigin;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoutingPolicyRepository extends JpaRepository<RoutingPolicyEntity, UUID> {

    @EntityGraph(attributePaths = "steps")
    List<RoutingPolicyEntity> findByEnabledTrueAndPolicyOriginOrderByPriorityAsc(PolicyOrigin origin);

    @EntityGraph(attributePaths = "steps")
    List<RoutingPolicyEntity> findAllByOrderByPriorityAsc();

    @EntityGraph(attributePaths = "steps")
    java.util.Optional<RoutingPolicyEntity> findWithStepsById(UUID id);

    boolean existsByCode(String code);
}
