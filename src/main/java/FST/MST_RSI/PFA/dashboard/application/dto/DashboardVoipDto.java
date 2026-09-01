package FST.MST_RSI.PFA.dashboard.application.dto;

import java.time.Instant;
import java.util.List;

public record DashboardVoipDto(
        Instant from,
        Instant to,
        VoipSummaryDto summary,
        List<VoipCallDto> recentCalls,
        List<LabelCountDto> byEscalationStep,
        List<VoipByRoleDto> byRole,
        List<VoipBySolutionDto> bySolution
) {
    public DashboardVoipDto(
            Instant from,
            Instant to,
            VoipSummaryDto summary,
            List<VoipCallDto> recentCalls,
            List<LabelCountDto> byEscalationStep
    ) {
        this(from, to, summary, recentCalls, byEscalationStep, List.of(), List.of());
    }
}
