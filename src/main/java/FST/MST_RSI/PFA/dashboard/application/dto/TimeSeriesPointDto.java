package FST.MST_RSI.PFA.dashboard.application.dto;

import java.time.Instant;

public record TimeSeriesPointDto(Instant bucket, long count) {
}
