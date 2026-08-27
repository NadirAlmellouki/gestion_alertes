package FST.MST_RSI.PFA.dashboard.application.usecase;

import FST.MST_RSI.PFA.dashboard.application.dto.DashboardAdminsDto;
import FST.MST_RSI.PFA.dashboard.infrastructure.persistence.DashboardProjectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class GetDashboardAdminsUseCase {

    private final DashboardProjectionRepository repository;

    public GetDashboardAdminsUseCase(DashboardProjectionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DashboardAdminsDto execute(Instant from, Instant to) {
        return new DashboardAdminsDto(
                from,
                to,
                repository.fetchAdminAvailability(from, to)
        );
    }
}
