package FST.MST_RSI.PFA.rulesengine.application.usecase;

import FST.MST_RSI.PFA.common.exception.BusinessException;
import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import FST.MST_RSI.PFA.rulesengine.application.dto.RuleDto;
import FST.MST_RSI.PFA.rulesengine.application.mapper.RuleDtoMapper;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRule;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleOrigin;
import FST.MST_RSI.PFA.rulesengine.domain.port.RuleRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UpdateBusinessRuleUseCase {

    private final RuleRepositoryPort ruleRepositoryPort;
    private final RuleDtoMapper ruleDtoMapper;

    public UpdateBusinessRuleUseCase(RuleRepositoryPort ruleRepositoryPort, RuleDtoMapper ruleDtoMapper) {
        this.ruleRepositoryPort = ruleRepositoryPort;
        this.ruleDtoMapper = ruleDtoMapper;
    }

    @Transactional
    public RuleDto execute(UUID id, RuleDto request) {
        BusinessRule existing = ruleRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business rule not found: " + id));
        if (existing.origin() == RuleOrigin.DEFAULT && request.origin() == RuleOrigin.CONFIGURED) {
            throw new BusinessException("READ_ONLY_DEFAULT", "Cannot change origin of a DEFAULT rule");
        }
        BusinessRule updated = ruleDtoMapper.toDomain(new RuleDto(
                id,
                request.code(),
                request.name(),
                request.description(),
                request.evaluationPriority(),
                request.enabled(),
                request.stopOnMatch(),
                existing.origin(),
                request.conditionGroups(),
                request.actions()
        ));
        return ruleDtoMapper.toDto(ruleRepositoryPort.save(updated));
    }
}
