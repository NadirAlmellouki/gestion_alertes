package FST.MST_RSI.PFA.alerting.application.dto;

import java.time.Instant;

public record AlertStakeholderDto(
        String personId,
        String fullName,
        String email,
        String phone,
        String extension,
        String role,
        String roleLabel,
        String unitType,
        String unitName,
        int hierarchyLevel,
        String sipReachability,
        Instant lastContactAt
) {
}
