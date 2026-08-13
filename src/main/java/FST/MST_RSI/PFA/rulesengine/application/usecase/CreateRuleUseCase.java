package FST.MST_RSI.PFA.rulesengine.application.usecase;

import FST.MST_RSI.PFA.common.exception.BusinessException;
import FST.MST_RSI.PFA.rulesengine.application.dto.RuleDto;
import FST.MST_RSI.PFA.rulesengine.application.mapper.RuleDtoMapper;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRule;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleOrigin;
import FST.MST_RSI.PFA.rulesengine.domain.port.RuleRepositoryPort;
import FST.MST_RSI.PFA.rulesengine.infrastructure.persistence.BusinessRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateRuleUseCase {

    private final RuleRepositoryPort ruleRepositoryPort;
    private final BusinessRuleRepository businessRuleRepository;
    private final RuleDtoMapper ruleDtoMapper;

    public CreateRuleUseCase(
            RuleRepositoryPort ruleRepositoryPort,
            BusinessRuleRepository businessRuleRepository,
            RuleDtoMapper ruleDtoMapper
    ) {
        this.ruleRepositoryPort = ruleRepositoryPort;
        this.businessRuleRepository = businessRuleRepository;
        this.ruleDtoMapper = ruleDtoMapper;
    }

    @Transactional
    public RuleDto execute(RuleDto request) {
        if (businessRuleRepository.existsByCode(request.code())) {
            throw new BusinessException("RULE_CODE_EXISTS", "A rule with code '" + request.code() + "' already exists");
        }
        BusinessRule rule = ruleDtoMapper.toDomain(request);
        if (rule.origin() == RuleOrigin.DEFAULT) {
            throw new BusinessException("INVALID_ORIGIN", "Ops cannot create DEFAULT rules via API");
        }
        return ruleDtoMapper.toDto(ruleRepositoryPort.save(rule));
    }
}
