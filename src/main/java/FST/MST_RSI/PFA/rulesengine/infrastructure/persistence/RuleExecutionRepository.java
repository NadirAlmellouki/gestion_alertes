package FST.MST_RSI.PFA.rulesengine.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RuleExecutionRepository extends JpaRepository<RuleExecutionEntity, UUID> {
}
