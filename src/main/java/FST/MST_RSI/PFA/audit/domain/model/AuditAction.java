package FST.MST_RSI.PFA.audit.domain.model;

public final class AuditAction {

    public static final String ALERT_RECEIVED = "ALERT_RECEIVED";
    public static final String ALERT_UPDATED = "ALERT_UPDATED";
    public static final String CLASSIFICATION_COMPLETED = "CLASSIFICATION_COMPLETED";
    public static final String RULE_EVALUATED = "RULE_EVALUATED";
    public static final String ROUTING_DECIDED = "ROUTING_DECIDED";
    public static final String NOTIFICATION_ATTEMPTED = "NOTIFICATION_ATTEMPTED";
    public static final String ESCALATION_SCHEDULED = "ESCALATION_SCHEDULED";
    public static final String ESCALATION_PROCESSED = "ESCALATION_PROCESSED";
    public static final String RESOLUTION_CHECK_SCHEDULED = "RESOLUTION_CHECK_SCHEDULED";
    public static final String RESOLUTION_CHECK_COMPLETED = "RESOLUTION_CHECK_COMPLETED";
    public static final String PIPELINE_COMPLETED = "PIPELINE_COMPLETED";
    public static final String MANUAL_CALL = "MANUAL_CALL";
    public static final String VOICE_CALL_REQUESTED = "VOICE_CALL_REQUESTED";
    public static final String VOICE_CALL_RINGING = "VOICE_CALL_RINGING";
    public static final String VOICE_CALL_ANSWERED = "VOICE_CALL_ANSWERED";
    public static final String VOICE_CALL_REJECTED = "VOICE_CALL_REJECTED";
    public static final String VOICE_CALL_BUSY = "VOICE_CALL_BUSY";
    public static final String VOICE_CALL_NO_ANSWER = "VOICE_CALL_NO_ANSWER";
    public static final String VOICE_CALL_HANGUP = "VOICE_CALL_HANGUP";
    public static final String VOICE_CALL_FAILED = "VOICE_CALL_FAILED";

    private AuditAction() {
    }
}
