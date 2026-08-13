package FST.MST_RSI.PFA.rulesengine.domain.port;

import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRule;
import FST.MST_RSI.PFA.rulesengine.domain.model.RuleOrigin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RuleRepositoryPort {

    List<BusinessRule> findEnabledByOriginOrderByPriority(RuleOrigin origin);

    Optional<BusinessRule> findById(UUID id);

    BusinessRule save(BusinessRule rule);

    void deleteById(UUID id);

    List<BusinessRule> findAll();
}
