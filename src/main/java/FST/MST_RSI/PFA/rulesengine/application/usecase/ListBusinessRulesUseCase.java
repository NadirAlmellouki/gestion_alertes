package FST.MST_RSI.PFA.rulesengine.application.usecase;

import FST.MST_RSI.PFA.rulesengine.application.dto.RuleDto;
import FST.MST_RSI.PFA.rulesengine.application.mapper.RuleDtoMapper;
import FST.MST_RSI.PFA.rulesengine.domain.port.RuleRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListBusinessRulesUseCase {

    private final RuleRepositoryPort ruleRepositoryPort;
    private final RuleDtoMapper ruleDtoMapper;

    public ListBusinessRulesUseCase(RuleRepositoryPort ruleRepositoryPort, RuleDtoMapper ruleDtoMapper) {
        this.ruleRepositoryPort = ruleRepositoryPort;
        this.ruleDtoMapper = ruleDtoMapper;
    }

    @Transactional(readOnly = true)
    public List<RuleDto> execute() {
        return ruleRepositoryPort.findAll().stream().map(ruleDtoMapper::toDto).toList();
    }
}
