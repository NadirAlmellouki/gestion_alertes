package FST.MST_RSI.PFA.dashboard.application.dto;

public record VoipSummaryDto(
        long total,
        long answered,
        long noAnswer,
        long rejected,
        long busy,
        long failed,
        double responseRate,
        double failureRate,
        Double avgDurationSeconds,
        Integer minDurationSeconds,
        Integer maxDurationSeconds,
        Double avgRingTimeSeconds,
        long manualCallsCount,
        long autoCallsCount
) {
    public VoipSummaryDto(long total, long sent, long failed, long deferred, long pending) {
        this(
                total,
                sent,
                deferred,
                0L,
                0L,
                failed,
                total > 0 ? Math.round((double) sent / total * 1000.0) / 10.0 : 0.0,
                total > 0 ? Math.round((double) failed / total * 1000.0) / 10.0 : 0.0,
                null,
                null,
                null,
                null,
                0L,
                total
        );
    }
}
