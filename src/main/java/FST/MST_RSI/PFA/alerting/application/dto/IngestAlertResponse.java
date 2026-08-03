package FST.MST_RSI.PFA.alerting.application.dto;

public record IngestAlertResponse(
        String alertId,
        String externalProblemId,
        boolean created
) {
}
