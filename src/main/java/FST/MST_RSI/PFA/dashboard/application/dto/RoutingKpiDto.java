package FST.MST_RSI.PFA.dashboard.application.dto;

public record RoutingKpiDto(
        long totalExecutions,
        long completed,
        long awaitingEscalation,
        long noPerson,
        long escalationSteps
) {
}
