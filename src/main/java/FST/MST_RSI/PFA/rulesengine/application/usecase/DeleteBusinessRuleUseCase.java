package FST.MST_RSI.PFA.rulesengine.application.usecase;

import FST.MST_RSI.PFA.common.exception.BusinessException;
import FST.MST_RSI.PFA.common.exception.ResourceNotFoundException;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRule;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleOrigin;
import FST.MST_RSI.PFA.rulesengine.domain.port.RuleRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteBusinessRuleUseCase {

    private final RuleRepositoryPort ruleRepositoryPort;

    public DeleteBusinessRuleUseCase(RuleRepositoryPort ruleRepositoryPort) {
        this.ruleRepositoryPort = ruleRepositoryPort;
    }

    @Transactional
    public void execute(UUID id) {
        BusinessRule rule = ruleRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business rule not found: " + id));
        if (rule.origin() == RuleOrigin.DEFAULT) {
            throw new BusinessException("READ_ONLY_DEFAULT", "DEFAULT rules cannot be deleted");
        }
        ruleRepositoryPort.deleteById(id);
    }
}
