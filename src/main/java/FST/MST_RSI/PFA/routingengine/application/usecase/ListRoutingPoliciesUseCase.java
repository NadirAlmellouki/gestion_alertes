package FST.MST_RSI.PFA.routingengine.application.usecase;

import FST.MST_RSI.PFA.routingengine.application.dto.RoutingPolicyDto;
import FST.MST_RSI.PFA.routingengine.application.mapper.RoutingPolicyDtoMapper;
import FST.MST_RSI.PFA.routingengine.domain.port.RoutingPolicyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListRoutingPoliciesUseCase {

    private final RoutingPolicyRepositoryPort routingPolicyRepositoryPort;
    private final RoutingPolicyDtoMapper routingPolicyDtoMapper;

    public ListRoutingPoliciesUseCase(
            RoutingPolicyRepositoryPort routingPolicyRepositoryPort,
            RoutingPolicyDtoMapper routingPolicyDtoMapper
    ) {
        this.routingPolicyRepositoryPort = routingPolicyRepositoryPort;
        this.routingPolicyDtoMapper = routingPolicyDtoMapper;
    }

    @Transactional(readOnly = true)
    public List<RoutingPolicyDto> execute() {
        return routingPolicyRepositoryPort.findAll().stream()
                .map(routingPolicyDtoMapper::toDto)
                .toList();
    }
}
