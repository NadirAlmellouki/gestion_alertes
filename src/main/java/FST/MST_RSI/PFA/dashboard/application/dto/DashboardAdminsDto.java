package FST.MST_RSI.PFA.dashboard.application.dto;

import java.time.Instant;
import java.util.List;

public record DashboardAdminsDto(
        Instant from,
        Instant to,
        List<AdminAvailabilityDto> admins
) {
}
