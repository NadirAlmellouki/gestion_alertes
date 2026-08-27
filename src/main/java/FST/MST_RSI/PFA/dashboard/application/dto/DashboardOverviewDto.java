package FST.MST_RSI.PFA.dashboard.application.dto;

import java.time.Instant;
import java.util.List;

public record DashboardOverviewDto(
        Instant from,
        Instant to,
        AlertKpiDto alerts,
        NotificationKpiDto notifications,
        RoutingKpiDto routing,
        ResolutionKpiDto resolution,
        ClassificationKpiDto classification,
        List<LabelCountDto> alertsBySeverity,
        List<LabelCountDto> alertsByCategory,
        List<TimeSeriesPointDto> alertTrend
) {
}
