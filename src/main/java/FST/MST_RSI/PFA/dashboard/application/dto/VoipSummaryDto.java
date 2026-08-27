package FST.MST_RSI.PFA.dashboard.application.dto;

public record VoipSummaryDto(
        long total,
        long sent,
        long failed,
        long deferred,
        long pending
) {
}
