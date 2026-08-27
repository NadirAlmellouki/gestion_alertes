package FST.MST_RSI.PFA.dashboard.application.usecase;

import FST.MST_RSI.PFA.dashboard.application.dto.DashboardVoipDto;
import FST.MST_RSI.PFA.dashboard.infrastructure.persistence.DashboardProjectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class GetDashboardVoipUseCase {

    private static final int RECENT_CALLS_LIMIT = 50;

    private final DashboardProjectionRepository repository;

    public GetDashboardVoipUseCase(DashboardProjectionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DashboardVoipDto execute(Instant from, Instant to) {
        return new DashboardVoipDto(
                from,
                to,
                repository.fetchVoipSummary(from, to),
                repository.fetchRecentVoipCalls(from, to, RECENT_CALLS_LIMIT),
                repository.fetchVoipByEscalationStep(from, to)
        );
    }
}
