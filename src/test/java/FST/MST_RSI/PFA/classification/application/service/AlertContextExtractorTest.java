package FST.MST_RSI.PFA.classification.application.service;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AlertContextExtractorTest {

    private final AlertContextExtractor extractor = new AlertContextExtractor(new ObjectMapper());

    @Test
    void extractsCompactContextFromProblemsV2Payload() throws Exception {
        String payload = new String(getClass().getResourceAsStream("/fixtures/dynatrace-problem.json").readAllBytes());
        Alert alert = Alert.createNew(
                "1234567890123456789",
                "Saturation CPU détectée sur PayCore",
                "PayCore",
                "Production",
                "RESOURCE_CONTENTION",
                "APPLICATION",
                "OPEN",
                null,
                "prd-app-paycore-01.bank.internal",
                payload,
                Instant.ofEpochMilli(1722000000000L)
        );

        var context = extractor.extract(alert);

        assertThat(context.title()).contains("PayCore");
        assertThat(context.severityLevel()).isEqualTo("RESOURCE_CONTENTION");
        assertThat(context.affectedEntityNames()).contains("PayCore");
        assertThat(context.rootCauseEntity()).contains("paycore");
        assertThat(context.k8sNamespaces()).contains("paycore");
        assertThat(context.evidenceSummary()).isNotBlank();
    }
}
