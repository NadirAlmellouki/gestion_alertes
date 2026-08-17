package FST.MST_RSI.PFA.routingengine.domain.model;

public final class RoutingExecutionStatus {

    public static final String STARTED = "STARTED";
    public static final String AWAITING_ESCALATION = "AWAITING_ESCALATION";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String NO_PERSON = "NO_PERSON";
    public static final String COMPLETED = "COMPLETED";
    public static final String EXPIRED = "EXPIRED";

    private RoutingExecutionStatus() {
    }
}
