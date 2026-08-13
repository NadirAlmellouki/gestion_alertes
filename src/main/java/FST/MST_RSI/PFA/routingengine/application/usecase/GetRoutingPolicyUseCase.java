package FST.MST_RSI.PFA.routingengine.application.usecase;

import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import FST.MST_RSI.PFA.routingengine.application.dto.RoutingPolicyDto;
import FST.MST_RSI.PFA.routingengine.application.mapper.RoutingPolicyDtoMapper;
import FST.MST_RSI.PFA.routingengine.domain.port.RoutingPolicyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetRoutingPolicyUseCase {

    private final RoutingPolicyRepositoryPort routingPolicyRepositoryPort;
    private final RoutingPolicyDtoMapper routingPolicyDtoMapper;

    public GetRoutingPolicyUseCase(
            RoutingPolicyRepositoryPort routingPolicyRepositoryPort,
            RoutingPolicyDtoMapper routingPolicyDtoMapper
    ) {
        this.routingPolicyRepositoryPort = routingPolicyRepositoryPort;
        this.routingPolicyDtoMapper = routingPolicyDtoMapper;
    }

    @Transactional(readOnly = true)
    public RoutingPolicyDto execute(UUID id) {
        return routingPolicyRepositoryPort.findById(id)
                .map(routingPolicyDtoMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Routing policy not found: " + id));
    }
}
