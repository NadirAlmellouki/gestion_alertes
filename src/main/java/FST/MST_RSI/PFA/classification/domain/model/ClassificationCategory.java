package FST.MST_RSI.PFA.classification.domain.model;

/**
 * Categories aligned with Dynatrace severityLevel + business needs from the référentiel.
 */
public enum ClassificationCategory {
    AVAILABILITY,
    ERROR,
    PERFORMANCE,
    RESOURCE_CONTENTION,
    SECURITY,
    CUSTOM_ALERT,
    UNKNOWN
}
