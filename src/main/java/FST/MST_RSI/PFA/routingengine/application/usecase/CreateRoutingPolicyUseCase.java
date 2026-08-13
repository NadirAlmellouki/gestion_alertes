package FST.MST_RSI.PFA.routingengine.application.usecase;

import FST.MST_RSI.PFA.common.exception.BusinessException;
import FST.MST_RSI.PFA.routingengine.application.dto.RoutingPolicyDto;
import FST.MST_RSI.PFA.routingengine.application.mapper.RoutingPolicyDtoMapper;
import FST.MST_RSI.PFA.routingengine.domain.model.PolicyOrigin;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingPolicy;
import FST.MST_RSI.PFA.routingengine.domain.port.RoutingPolicyRepositoryPort;
import FST.MST_RSI.PFA.routingengine.infrastructure.persistence.RoutingPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateRoutingPolicyUseCase {

    private final RoutingPolicyRepositoryPort routingPolicyRepositoryPort;
    private final RoutingPolicyRepository routingPolicyRepository;
    private final RoutingPolicyDtoMapper routingPolicyDtoMapper;

    public CreateRoutingPolicyUseCase(
            RoutingPolicyRepositoryPort routingPolicyRepositoryPort,
            RoutingPolicyRepository routingPolicyRepository,
            RoutingPolicyDtoMapper routingPolicyDtoMapper
    ) {
        this.routingPolicyRepositoryPort = routingPolicyRepositoryPort;
        this.routingPolicyRepository = routingPolicyRepository;
        this.routingPolicyDtoMapper = routingPolicyDtoMapper;
    }

    @Transactional
    public RoutingPolicyDto execute(RoutingPolicyDto request) {
        if (routingPolicyRepository.existsByCode(request.code())) {
            throw new BusinessException("POLICY_CODE_EXISTS", "A policy with code '" + request.code() + "' already exists");
        }
        RoutingPolicy policy = routingPolicyDtoMapper.toDomain(request);
        if (policy.origin() == PolicyOrigin.DEFAULT) {
            throw new BusinessException("INVALID_ORIGIN", "Ops cannot create DEFAULT policies via API");
        }
        return routingPolicyDtoMapper.toDto(routingPolicyRepositoryPort.save(policy));
    }
}
