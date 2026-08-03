package FST.MST_RSI.PFA.alerting.domain.service;

import FST.MST_RSI.PFA.alerting.domain.model.RawAlertPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAlertPayloadValidatorTest {

    private DefaultAlertPayloadValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DefaultAlertPayloadValidator(new ObjectMapper());
    }

    @Test
    void validDynatracePayloadIsAccepted() throws Exception {
        String json = new String(getClass().getResourceAsStream("/fixtures/dynatrace-problem.json").readAllBytes());
        var result = validator.validate(new RawAlertPayload(json));

        assertThat(result.valid()).isTrue();
    }

    @Test
    void missingProblemIdentifierIsRejected() {
        var result = validator.validate(new RawAlertPayload("{\"ProblemTitle\":\"Test alert\"}"));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(error -> error.contains("identifier"));
    }

    @Test
    void invalidJsonIsRejected() {
        var result = validator.validate(new RawAlertPayload("{not-json"));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).contains("Payload is not valid JSON");
    }
}
