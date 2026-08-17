package FST.MST_RSI.PFA.notification.application.service;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationCategory;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationStatus;
import FST.MST_RSI.PFA.common.domain.vo.Confidence;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VoipMessageComposerTest {

    private final VoipMessageComposer composer = new VoipMessageComposer();

    @Test
    void includesAlertContextInMessage() {
        Alert alert = Alert.createNew(
                "P-999", "CPU saturation", "PayCore", "PROD", "CRITICAL", "APPLICATION",
                "OPEN", "http://dynatrace/problem/1", "host-1", "{}", Instant.now()
        );
        ClassificationResult classification = new ClassificationResult(
                ClassificationCategory.RESOURCE_CONTENTION, "CPU", new Confidence(0.9),
                "PayCore", "Paiements", "Pilotage", "Core", "s", "c", "j", List.of(), false,
                ClassificationStatus.SUCCESS, null, null
        );

        String message = composer.compose(alert, classification, "Jane Doe");

        assertThat(message).contains("Jane Doe");
        assertThat(message).contains("P-999");
        assertThat(message).contains("PayCore");
        assertThat(message).contains("CPU saturation");
    }
}
