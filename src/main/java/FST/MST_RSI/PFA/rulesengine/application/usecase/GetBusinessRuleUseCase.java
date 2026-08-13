package FST.MST_RSI.PFA.rulesengine.application.usecase;

import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import FST.MST_RSI.PFA.rulesengine.application.dto.RuleDto;
import FST.MST_RSI.PFA.rulesengine.application.mapper.RuleDtoMapper;
import FST.MST_RSI.PFA.rulesengine.domain.port.RuleRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetBusinessRuleUseCase {

    private final RuleRepositoryPort ruleRepositoryPort;
    private final RuleDtoMapper ruleDtoMapper;

    public GetBusinessRuleUseCase(RuleRepositoryPort ruleRepositoryPort, RuleDtoMapper ruleDtoMapper) {
        this.ruleRepositoryPort = ruleRepositoryPort;
        this.ruleDtoMapper = ruleDtoMapper;
    }

    @Transactional(readOnly = true)
    public RuleDto execute(UUID id) {
        return ruleRepositoryPort.findById(id)
                .map(ruleDtoMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Business rule not found: " + id));
    }
}
