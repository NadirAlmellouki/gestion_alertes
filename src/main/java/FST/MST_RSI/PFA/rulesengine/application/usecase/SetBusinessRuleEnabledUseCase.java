package FST.MST_RSI.PFA.rulesengine.application.usecase;

import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import FST.MST_RSI.PFA.rulesengine.application.dto.RuleDto;
import FST.MST_RSI.PFA.rulesengine.application.mapper.RuleDtoMapper;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRule;
import FST.MST_RSI.PFA.rulesengine.domain.port.RuleRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SetBusinessRuleEnabledUseCase {

    private final RuleRepositoryPort ruleRepositoryPort;
    private final RuleDtoMapper ruleDtoMapper;

    public SetBusinessRuleEnabledUseCase(RuleRepositoryPort ruleRepositoryPort, RuleDtoMapper ruleDtoMapper) {
        this.ruleRepositoryPort = ruleRepositoryPort;
        this.ruleDtoMapper = ruleDtoMapper;
    }

    @Transactional
    public RuleDto execute(UUID id, boolean enabled) {
        BusinessRule existing = ruleRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business rule not found: " + id));
        BusinessRule updated = new BusinessRule(
                existing.id(),
                existing.code(),
                existing.name(),
                existing.description(),
                existing.evaluationPriority(),
                enabled,
                existing.stopOnMatch(),
                existing.origin(),
                existing.conditionGroups(),
                existing.actions()
        );
        return ruleDtoMapper.toDto(ruleRepositoryPort.save(updated));
    }
}
