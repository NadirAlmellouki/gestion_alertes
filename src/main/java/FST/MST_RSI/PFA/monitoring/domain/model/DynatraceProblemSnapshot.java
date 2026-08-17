package FST.MST_RSI.PFA.monitoring.domain.model;

public record DynatraceProblemSnapshot(
        String problemId,
        String status
) {
    public boolean isResolved() {
        return "RESOLVED".equalsIgnoreCase(status);
    }

    public boolean isOpen() {
        return "OPEN".equalsIgnoreCase(status);
    }
}
