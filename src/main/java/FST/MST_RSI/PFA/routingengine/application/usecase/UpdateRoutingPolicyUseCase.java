package FST.MST_RSI.PFA.routingengine.application.usecase;

import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import FST.MST_RSI.PFA.routingengine.application.dto.RoutingPolicyDto;
import FST.MST_RSI.PFA.routingengine.application.mapper.RoutingPolicyDtoMapper;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingPolicy;
import FST.MST_RSI.PFA.routingengine.domain.port.RoutingPolicyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UpdateRoutingPolicyUseCase {

    private final RoutingPolicyRepositoryPort routingPolicyRepositoryPort;
    private final RoutingPolicyDtoMapper routingPolicyDtoMapper;

    public UpdateRoutingPolicyUseCase(
            RoutingPolicyRepositoryPort routingPolicyRepositoryPort,
            RoutingPolicyDtoMapper routingPolicyDtoMapper
    ) {
        this.routingPolicyRepositoryPort = routingPolicyRepositoryPort;
        this.routingPolicyDtoMapper = routingPolicyDtoMapper;
    }

    @Transactional
    public RoutingPolicyDto execute(UUID id, RoutingPolicyDto request) {
        RoutingPolicy existing = routingPolicyRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Routing policy not found: " + id));
        RoutingPolicy updated = routingPolicyDtoMapper.toDomain(new RoutingPolicyDto(
                id,
                request.code(),
                request.name(),
                request.description(),
                request.enabled(),
                request.priority(),
                existing.origin(),
                request.steps()
        ));
        return routingPolicyDtoMapper.toDto(routingPolicyRepositoryPort.save(updated));
    }
}
