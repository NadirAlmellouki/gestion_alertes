package FST.MST_RSI.PFA.alerting.domain.service;

import FST.MST_RSI.PFA.alerting.domain.model.RawAlertPayload;
import FST.MST_RSI.PFA.alerting.domain.model.ValidationResult;

public interface AlertPayloadValidator {

    ValidationResult validate(RawAlertPayload payload);
}
