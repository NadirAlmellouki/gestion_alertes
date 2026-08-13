package FST.MST_RSI.PFA.routingengine.application.usecase;

import FST.MST_RSI.PFA.common.exception.BusinessException;
import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import FST.MST_RSI.PFA.routingengine.domain.model.PolicyOrigin;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingPolicy;
import FST.MST_RSI.PFA.routingengine.domain.port.RoutingPolicyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteRoutingPolicyUseCase {

    private final RoutingPolicyRepositoryPort routingPolicyRepositoryPort;

    public DeleteRoutingPolicyUseCase(RoutingPolicyRepositoryPort routingPolicyRepositoryPort) {
        this.routingPolicyRepositoryPort = routingPolicyRepositoryPort;
    }

    @Transactional
    public void execute(UUID id) {
        RoutingPolicy policy = routingPolicyRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Routing policy not found: " + id));
        if (policy.origin() == PolicyOrigin.DEFAULT) {
            throw new BusinessException("READ_ONLY_DEFAULT", "DEFAULT policies cannot be deleted");
        }
        routingPolicyRepositoryPort.deleteById(id);
    }
}
