package FST.MST_RSI.PFA.monitoring.domain.port;

import FST.MST_RSI.PFA.monitoring.domain.model.DynatraceProblemSnapshot;

import java.util.Optional;

public interface DynatraceProblemPort {

    Optional<DynatraceProblemSnapshot> fetchProblem(String externalProblemId);
}
