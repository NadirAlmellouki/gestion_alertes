package FST.MST_RSI.PFA.classification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AlertLlmAnalysisRepository extends JpaRepository<AlertLlmAnalysisEntity, UUID> {

    Optional<AlertLlmAnalysisEntity> findTopByAlertIdOrderByCreatedAtDesc(UUID alertId);
}
