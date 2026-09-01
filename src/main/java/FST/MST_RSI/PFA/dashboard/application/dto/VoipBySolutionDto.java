package FST.MST_RSI.PFA.dashboard.application.dto;

public record VoipBySolutionDto(
        String solutionName,
        long totalAlerts,
        long totalVoipCalls,
        long answeredCalls,
        double responseRate
) {
}
