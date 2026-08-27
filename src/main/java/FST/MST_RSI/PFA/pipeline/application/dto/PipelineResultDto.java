package FST.MST_RSI.PFA.pipeline.application.dto;

public record PipelineResultDto(
        String alertId,
        String classificationStatus,
        String category,
        Double confidence,
        boolean humanValidationRequired,
        String matchedRuleCode,
        boolean routingTriggered,
        String routingStatus,
        String policyCode,
        String selectedPersonName,
        String channel,
        String notificationOutcome,
        String notificationDetail
) {
}
