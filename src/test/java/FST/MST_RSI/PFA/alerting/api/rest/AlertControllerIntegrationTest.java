package FST.MST_RSI.PFA.alerting.api.rest;

import FST.MST_RSI.PFA.alerting.application.usecase.IngestAlertUseCase;
import FST.MST_RSI.PFA.security.domain.Role;
import FST.MST_RSI.PFA.security.infrastructure.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AlertControllerIntegrationTest {

    private static final String INGESTION_TOKEN = "test-ingestion-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IngestAlertUseCase ingestAlertUseCase;

    @Autowired
    private JwtTokenService jwtTokenService;

    private String alertId;

    @BeforeEach
    void setUp() throws Exception {
        String payload = new String(getClass().getResourceAsStream("/fixtures/dynatrace-problem.json").readAllBytes());
        var result = ingestAlertUseCase.execute(payload);
        alertId = result.alert().getId().value().toString();
    }

    @Test
    void listRecentRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/alerts/recent"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operatorCanReadRecentAlerts() throws Exception {
        String token = jwtTokenService.generateToken("operateur1", Role.OPERATEUR);

        mockMvc.perform(get("/api/v1/alerts/recent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(alertId))
                .andExpect(jsonPath("$[0].notificationState").value("EN_ATTENTE"));
    }

    @Test
    void operatorCanReadAlertDetail() throws Exception {
        String token = jwtTokenService.generateToken("operateur1", Role.OPERATEUR);

        mockMvc.perform(get("/api/v1/alerts/" + alertId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Saturation CPU détectée sur PayCore"))
                .andExpect(jsonPath("$.timeline[0].eventType").value("RECEIVED"));
    }

    @Test
    void ingestEndpointRemainsAccessibleWithServiceToken() throws Exception {
        String payload = new String(getClass().getResourceAsStream("/fixtures/dynatrace-problem.json").readAllBytes());

        mockMvc.perform(post("/api/v1/ingestion/dynatrace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Ingestion-Token", INGESTION_TOKEN)
                        .content(payload))
                .andExpect(status().isCreated());
    }
}
