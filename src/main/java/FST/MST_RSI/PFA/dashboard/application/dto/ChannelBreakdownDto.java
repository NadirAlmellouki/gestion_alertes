package FST.MST_RSI.PFA.dashboard.application.dto;

public record ChannelBreakdownDto(
        String channel,
        long sent,
        long failed,
        long deferred,
        long pending,
        long skipped
) {
}
