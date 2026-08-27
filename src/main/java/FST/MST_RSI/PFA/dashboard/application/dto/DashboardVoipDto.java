package FST.MST_RSI.PFA.dashboard.application.dto;

import java.time.Instant;
import java.util.List;

public record DashboardVoipDto(
        Instant from,
        Instant to,
        VoipSummaryDto summary,
        List<VoipCallDto> recentCalls,
        List<LabelCountDto> byEscalationStep
) {
}
