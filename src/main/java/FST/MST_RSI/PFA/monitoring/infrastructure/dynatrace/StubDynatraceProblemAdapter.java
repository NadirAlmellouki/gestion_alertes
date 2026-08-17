package FST.MST_RSI.PFA.monitoring.infrastructure.dynatrace;

import FST.MST_RSI.PFA.monitoring.domain.model.DynatraceProblemSnapshot;
import FST.MST_RSI.PFA.monitoring.domain.port.DynatraceProblemPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "app.monitoring.dynatrace", name = "enabled", havingValue = "false", matchIfMissing = true)
public class StubDynatraceProblemAdapter implements DynatraceProblemPort {

    @Override
    public Optional<DynatraceProblemSnapshot> fetchProblem(String externalProblemId) {
        return Optional.of(new DynatraceProblemSnapshot(externalProblemId, "OPEN"));
    }
}
