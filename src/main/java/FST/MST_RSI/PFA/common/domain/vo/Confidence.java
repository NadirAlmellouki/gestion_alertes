package FST.MST_RSI.PFA.common.domain.vo;

public record Confidence(double value) {
    public Confidence {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
    }
}
