package FST.MST_RSI.PFA.rulesengine.infrastructure.persistence;

import FST.MST_RSI.PFA.rulesengine.domain.model.RuleOrigin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessRuleRepository extends JpaRepository<BusinessRuleEntity, UUID> {

    @Query("""
            SELECT DISTINCT r FROM BusinessRuleEntity r
            LEFT JOIN FETCH r.conditionGroups
            WHERE r.enabled = TRUE AND r.ruleOrigin = :origin
            ORDER BY r.evaluationPriority ASC
            """)
    List<BusinessRuleEntity> findByEnabledTrueAndRuleOriginOrderByEvaluationPriorityAsc(@Param("origin") RuleOrigin origin);

    @Query("""
            SELECT DISTINCT r FROM BusinessRuleEntity r
            LEFT JOIN FETCH r.conditionGroups
            ORDER BY r.evaluationPriority ASC
            """)
    List<BusinessRuleEntity> findAllByOrderByEvaluationPriorityAsc();

    @Query("""
            SELECT r FROM BusinessRuleEntity r
            LEFT JOIN FETCH r.conditionGroups
            WHERE r.id = :id
            """)
    Optional<BusinessRuleEntity> findWithDetailsById(@Param("id") UUID id);

    boolean existsByCode(String code);
}
