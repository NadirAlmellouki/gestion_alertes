package FST.MST_RSI.PFA.alerting.application.dto;

import java.util.List;

public record IngestAlertErrorResponse(
        String code,
        List<String> errors
) {
}
