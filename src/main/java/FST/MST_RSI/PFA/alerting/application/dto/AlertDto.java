package FST.MST_RSI.PFA.alerting.application.dto;

import FST.MST_RSI.PFA.alerting.domain.model.NotificationState;

import java.time.Instant;

public record AlertDto(
        String id,
        String externalProblemId,
        String title,
        String applicationName,
        String environment,
        String severity,
        String impact,
        String dynatraceState,
        NotificationState notificationState,
        String problemUrl,
        String hostName,
        Instant receivedAt,
        Instant problemStartedAt,
        String classificationStatus,
        String classificationCategory,
        String problemType,
        Double confidence,
        Boolean requiresHumanValidation,
        String matchedSolution,
        String matchedDomain,
        String matchedPole,
        String matchedEntity,
        String resolvedPsi,
        String summary,
        String probableCause,
        String justification
) {
}
