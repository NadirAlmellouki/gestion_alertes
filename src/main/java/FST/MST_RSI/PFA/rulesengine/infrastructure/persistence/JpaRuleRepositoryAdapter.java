package FST.MST_RSI.PFA.rulesengine.infrastructure.persistence;

import FST.MST_RSI.PFA.rulesengine.application.mapper.BusinessRuleMapper;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRule;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleOrigin;
import FST.MST_RSI.PFA.rulesengine.domain.port.RuleRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaRuleRepositoryAdapter implements RuleRepositoryPort {

    private final BusinessRuleRepository repository;
    private final BusinessRuleMapper mapper;

    public JpaRuleRepositoryAdapter(BusinessRuleRepository repository, BusinessRuleMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessRule> findEnabledByOriginOrderByPriority(RuleOrigin origin) {
        return repository.findByEnabledTrueAndRuleOriginOrderByEvaluationPriorityAsc(origin).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BusinessRule> findById(UUID id) {
        return repository.findWithDetailsById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public BusinessRule save(BusinessRule rule) {
        BusinessRuleEntity entity = repository.findById(rule.id()).orElseGet(BusinessRuleEntity::new);
        mapper.mapToEntity(rule, entity);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessRule> findAll() {
        return repository.findAllByOrderByEvaluationPriorityAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
