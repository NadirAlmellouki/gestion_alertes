package FST.MST_RSI.PFA.routingengine.domain.port;

import FST.MST_RSI.PFA.routingengine.domain.model.PolicyOrigin;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingPolicy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoutingPolicyRepositoryPort {

    List<RoutingPolicy> findEnabledByOrigin(PolicyOrigin origin);

    List<RoutingPolicy> findAll();

    Optional<RoutingPolicy> findById(UUID id);

    RoutingPolicy save(RoutingPolicy policy);

    void deleteById(UUID id);
}
