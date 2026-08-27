package FST.MST_RSI.PFA.dashboard.application.usecase;

import FST.MST_RSI.PFA.dashboard.application.dto.DashboardOverviewDto;
import FST.MST_RSI.PFA.dashboard.infrastructure.persistence.DashboardProjectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class GetDashboardOverviewUseCase {

    private final DashboardProjectionRepository repository;

    public GetDashboardOverviewUseCase(DashboardProjectionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DashboardOverviewDto execute(Instant from, Instant to) {
        return new DashboardOverviewDto(
                from,
                to,
                repository.fetchAlertKpis(from, to),
                repository.fetchNotificationKpis(from, to),
                repository.fetchRoutingKpis(from, to),
                repository.fetchResolutionKpis(from, to),
                repository.fetchClassificationKpis(from, to),
                repository.fetchAlertsBySeverity(from, to),
                repository.fetchAlertsByCategory(from, to),
                repository.fetchAlertTrend(from, to)
        );
    }
}
