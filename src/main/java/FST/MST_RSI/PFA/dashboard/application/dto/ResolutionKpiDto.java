package FST.MST_RSI.PFA.dashboard.application.dto;

public record ResolutionKpiDto(
        long totalChecks,
        long resolved,
        long active,
        long expired,
        long error
) {
}
