package FST.MST_RSI.PFA.dashboard.application.dto;

public record VoipByRoleDto(
        String role,
        long totalCalls,
        long answeredCalls,
        double responseRate,
        Double avgDurationSeconds
) {
}
