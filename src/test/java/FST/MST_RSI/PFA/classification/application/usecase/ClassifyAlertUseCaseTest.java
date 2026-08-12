package FST.MST_RSI.PFA.classification.application.usecase;

import FST.MST_RSI.PFA.alerting.application.usecase.IngestAlertUseCase;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationCategory;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationResult;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationStatus;
import FST.MST_RSI.PFA.classification.domain.port.AlertClassifierPort;
import FST.MST_RSI.PFA.common.domain.vo.Confidence;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@SpringBootTest
class ClassifyAlertUseCaseTest {

    @Autowired
    private IngestAlertUseCase ingestAlertUseCase;

    @Autowired
    private ClassifyAlertUseCase classifyAlertUseCase;

    @MockitoBean
    private AlertClassifierPort alertClassifierPort;

    @Test
    void classifiesIngestedAlertUsingMockedLlm() throws Exception {
        String payload = new String(getClass().getResourceAsStream("/fixtures/dynatrace-problem.json").readAllBytes());
        var ingest = ingestAlertUseCase.execute(payload);
        String alertId = ingest.alert().getId().value().toString();

        when(alertClassifierPort.classify(any(), anyList())).thenReturn(new ClassificationResult(
                ClassificationCategory.RESOURCE_CONTENTION,
                "CPU_SATURATION",
                new Confidence(0.88),
                "PayCore",
                "Paiements",
                "Pilotage",
                "Core Processing & Services",
                "Saturation CPU PayCore",
                "Charge anormale",
                "Match référentiel PayCore",
                List.of(),
                false,
                ClassificationStatus.SUCCESS,
                null,
                null
        ));

        ClassificationResult result = classifyAlertUseCase.execute(alertId);

        assertThat(result.status()).isEqualTo(ClassificationStatus.SUCCESS);
        assertThat(result.matchedSolution()).isEqualTo("PayCore");
        assertThat(result.category()).isEqualTo(ClassificationCategory.RESOURCE_CONTENTION);
    }

    @Test
    void providerFailureIsConvertedToFallback() throws Exception {
        String payload = new String(getClass().getResourceAsStream("/fixtures/dynatrace-problem.json").readAllBytes());
        var ingest = ingestAlertUseCase.execute(payload);
        String alertId = ingest.alert().getId().value().toString();

        when(alertClassifierPort.classify(any(), anyList()))
                .thenReturn(ClassificationResult.fallback("LLM provider returned HTTP 503"));

        ClassificationResult result = classifyAlertUseCase.execute(alertId);

        assertThat(result.status()).isEqualTo(ClassificationStatus.FALLBACK);
        assertThat(result.requiresHumanValidation()).isTrue();
        assertThat(result.confidence().value()).isEqualTo(0.0);
    }
}
