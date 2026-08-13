package FST.MST_RSI.PFA.routingengine.infrastructure.persistence;

import FST.MST_RSI.PFA.routingengine.application.mapper.RoutingPolicyMapper;
import FST.MST_RSI.PFA.routingengine.domain.model.PolicyOrigin;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingPolicy;
import FST.MST_RSI.PFA.routingengine.domain.port.RoutingPolicyRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaRoutingPolicyRepositoryAdapter implements RoutingPolicyRepositoryPort {

    private final RoutingPolicyRepository repository;
    private final RoutingPolicyMapper mapper;

    public JpaRoutingPolicyRepositoryAdapter(RoutingPolicyRepository repository, RoutingPolicyMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoutingPolicy> findEnabledByOrigin(PolicyOrigin origin) {
        return repository.findByEnabledTrueAndPolicyOriginOrderByPriorityAsc(origin).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoutingPolicy> findAll() {
        return repository.findAllByOrderByPriorityAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RoutingPolicy> findById(UUID id) {
        return repository.findWithStepsById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public RoutingPolicy save(RoutingPolicy policy) {
        RoutingPolicyEntity entity = repository.findById(policy.id()).orElseGet(RoutingPolicyEntity::new);
        mapper.mapToEntity(policy, entity);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
