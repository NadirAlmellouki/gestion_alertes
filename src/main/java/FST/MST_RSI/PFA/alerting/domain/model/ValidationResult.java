package FST.MST_RSI.PFA.alerting.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ValidationResult(boolean valid, List<String> errors) {

    public ValidationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult failure(String error) {
        return new ValidationResult(false, List.of(error));
    }

    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, new ArrayList<>(errors));
    }

    public List<String> errors() {
        return Collections.unmodifiableList(errors);
    }
}
