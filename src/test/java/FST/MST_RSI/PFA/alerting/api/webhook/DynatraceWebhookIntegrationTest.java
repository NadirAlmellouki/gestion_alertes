package FST.MST_RSI.PFA.alerting.api.webhook;

import FST.MST_RSI.PFA.alerting.domain.model.NotificationState;
import FST.MST_RSI.PFA.alerting.infrastructure.persistence.AlertEntity;
import FST.MST_RSI.PFA.alerting.infrastructure.persistence.SpringDataAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DynatraceWebhookIntegrationTest {

    private static final String INGESTION_TOKEN = "test-ingestion-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringDataAlertRepository alertRepository;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
    }

    @Test
    void ingestValidPayloadCreatesAlertWithPendingNotificationState() throws Exception {
        String payload = new String(getClass().getResourceAsStream("/fixtures/dynatrace-problem.json").readAllBytes());

        mockMvc.perform(post("/api/v1/ingestion/dynatrace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Ingestion-Token", INGESTION_TOKEN)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalProblemId").value("1234567890123456789"))
                .andExpect(jsonPath("$.created").value(true));

        AlertEntity alert = alertRepository.findByProblemId("1234567890123456789").orElseThrow();
        assertThat(alert.getNotificationState()).isEqualTo(NotificationState.EN_ATTENTE);
    }

    @Test
    void ingestInvalidPayloadReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/dynatrace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Ingestion-Token", INGESTION_TOKEN)
                        .content("{\"ProblemTitle\":\"missing id\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAYLOAD"));
    }

    @Test
    void ingestWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/dynatrace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
