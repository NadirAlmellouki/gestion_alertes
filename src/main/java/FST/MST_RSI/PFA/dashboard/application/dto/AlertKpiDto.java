package FST.MST_RSI.PFA.dashboard.application.dto;

public record AlertKpiDto(
        long total,
        long open,
        long closed,
        long critical,
        long humanValidationRequired,
        double resolutionRatePercent
) {
}
