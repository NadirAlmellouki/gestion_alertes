package FST.MST_RSI.PFA.alerting.domain.model;

import java.util.UUID;

public record AlertId(UUID value) {
    public AlertId {
        if (value == null) {
            throw new IllegalArgumentException("AlertId cannot be null");
        }
    }

    public static AlertId generate() {
        return new AlertId(UUID.randomUUID());
    }

    public static AlertId of(String value) {
        return new AlertId(UUID.fromString(value));
    }
}
