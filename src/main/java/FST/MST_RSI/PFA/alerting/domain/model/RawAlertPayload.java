package FST.MST_RSI.PFA.alerting.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record RawAlertPayload(String jsonBody) {

    public RawAlertPayload {
        if (jsonBody == null || jsonBody.isBlank()) {
            throw new IllegalArgumentException("Payload JSON cannot be blank");
        }
    }
}
