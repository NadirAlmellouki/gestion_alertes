package FST.MST_RSI.PFA.security;

import FST.MST_RSI.PFA.security.domain.Role;
import FST.MST_RSI.PFA.security.infrastructure.JwtTokenService;
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
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/security-probe/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operatorTokenOnAdminEndpointReturns403() throws Exception {
        String token = jwtTokenService.generateToken("operateur1", Role.OPERATEUR);

        mockMvc.perform(get("/api/v1/security-probe/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminTokenOnAdminEndpointReturns200() throws Exception {
        String token = jwtTokenService.generateToken("admin1", Role.ADMINISTRATEUR);

        mockMvc.perform(get("/api/v1/security-probe/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("admin-ok"));
    }

    @Test
    void devLoginEndpointIsNotAvailableInTestProfile() throws Exception {
        mockMvc.perform(post("/dev/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"operateur1","role":"OPERATEUR"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
