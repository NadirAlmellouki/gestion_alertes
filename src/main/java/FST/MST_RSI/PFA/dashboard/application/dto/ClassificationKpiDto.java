package FST.MST_RSI.PFA.dashboard.application.dto;

public record ClassificationKpiDto(
        long classified,
        long fallback,
        long humanValidationRequired,
        Double avgDurationMs
) {
}
