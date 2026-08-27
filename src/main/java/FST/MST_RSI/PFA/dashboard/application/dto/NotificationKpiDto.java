package FST.MST_RSI.PFA.dashboard.application.dto;

import java.util.List;

public record NotificationKpiDto(
        long total,
        long sent,
        long failed,
        long deferred,
        long pending,
        List<ChannelBreakdownDto> byChannel
) {
}
