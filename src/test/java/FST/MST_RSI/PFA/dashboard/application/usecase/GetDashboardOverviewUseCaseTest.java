package FST.MST_RSI.PFA.dashboard.application.usecase;

import FST.MST_RSI.PFA.dashboard.application.dto.AlertKpiDto;
import FST.MST_RSI.PFA.dashboard.application.dto.ClassificationKpiDto;
import FST.MST_RSI.PFA.dashboard.application.dto.LabelCountDto;
import FST.MST_RSI.PFA.dashboard.application.dto.NotificationKpiDto;
import FST.MST_RSI.PFA.dashboard.application.dto.ResolutionKpiDto;
import FST.MST_RSI.PFA.dashboard.application.dto.RoutingKpiDto;
import FST.MST_RSI.PFA.dashboard.application.dto.TimeSeriesPointDto;
import FST.MST_RSI.PFA.dashboard.infrastructure.persistence.DashboardProjectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDashboardOverviewUseCaseTest {

    @Mock
    private DashboardProjectionRepository repository;

    @InjectMocks
    private GetDashboardOverviewUseCase useCase;

    @Test
    void aggregatesAllProjectionSlices() {
        Instant from = Instant.parse("2026-08-12T00:00:00Z");
        Instant to = Instant.parse("2026-08-19T00:00:00Z");

        when(repository.fetchAlertKpis(from, to)).thenReturn(new AlertKpiDto(10, 4, 6, 2, 1, 60.0));
        when(repository.fetchNotificationKpis(from, to)).thenReturn(new NotificationKpiDto(5, 4, 1, 0, 0, List.of()));
        when(repository.fetchRoutingKpis(from, to)).thenReturn(new RoutingKpiDto(3, 2, 1, 0, 4));
        when(repository.fetchResolutionKpis(from, to)).thenReturn(new ResolutionKpiDto(2, 1, 1, 0, 0));
        when(repository.fetchClassificationKpis(from, to)).thenReturn(new ClassificationKpiDto(8, 1, 1, 1200.0));
        when(repository.fetchAlertsBySeverity(from, to)).thenReturn(List.of(new LabelCountDto("ERROR", 2)));
        when(repository.fetchAlertsByCategory(from, to)).thenReturn(List.of(new LabelCountDto("INFRA", 3)));
        when(repository.fetchAlertTrend(from, to)).thenReturn(List.of(new TimeSeriesPointDto(from, 2)));

        var overview = useCase.execute(from, to);

        assertThat(overview.from()).isEqualTo(from);
        assertThat(overview.alerts().total()).isEqualTo(10);
        assertThat(overview.notifications().sent()).isEqualTo(4);
        assertThat(overview.routing().escalationSteps()).isEqualTo(4);
        assertThat(overview.alertsBySeverity()).hasSize(1);
        verify(repository).fetchAlertTrend(from, to);
    }
}
