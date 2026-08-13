package FST.MST_RSI.PFA.rulesengine.infrastructure.persistence;

import FST.MST_RSI.PFA.rulesengine.domain.model.RuleOrigin;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BusinessRuleRepository extends JpaRepository<BusinessRuleEntity, UUID> {

    @EntityGraph(attributePaths = {"conditionGroups", "conditionGroups.conditions", "actions"})
    List<BusinessRuleEntity> findByEnabledTrueAndRuleOriginOrderByEvaluationPriorityAsc(RuleOrigin origin);

    @EntityGraph(attributePaths = {"conditionGroups", "conditionGroups.conditions", "actions"})
    List<BusinessRuleEntity> findAllByOrderByEvaluationPriorityAsc();

    @EntityGraph(attributePaths = {"conditionGroups", "conditionGroups.conditions", "actions"})
    java.util.Optional<BusinessRuleEntity> findWithDetailsById(UUID id);

    boolean existsByCode(String code);
}
