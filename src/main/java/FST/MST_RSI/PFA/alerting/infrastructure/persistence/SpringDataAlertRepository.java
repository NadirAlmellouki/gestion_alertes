package FST.MST_RSI.PFA.alerting.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAlertRepository extends JpaRepository<AlertEntity, UUID> {

    Optional<AlertEntity> findByExternalProblemId(String externalProblemId);

    @EntityGraph(attributePaths = "timeline")
    Optional<AlertEntity> findWithTimelineByExternalProblemId(String externalProblemId);

    List<AlertEntity> findByReceivedAtAfterOrderByReceivedAtDesc(Instant since);

    Page<AlertEntity> findByReceivedAtBetweenOrderByReceivedAtDesc(Instant from, Instant to, Pageable pageable);

    @EntityGraph(attributePaths = "timeline")
    Optional<AlertEntity> findWithTimelineById(UUID id);
}
