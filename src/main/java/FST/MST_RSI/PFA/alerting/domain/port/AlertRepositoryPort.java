package FST.MST_RSI.PFA.alerting.domain.port;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.model.AlertId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertRepositoryPort {

    Alert save(Alert alert);

    Optional<Alert> findById(AlertId id);

    Optional<Alert> findByExternalProblemId(String externalProblemId);

    List<Alert> findRecentSince(Instant since);

    List<Alert> findHistory(Instant from, Instant to, int page, int size);
}
