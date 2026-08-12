package FST.MST_RSI.PFA.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/security-probe/supervisor"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void opsTokenOnSupervisorEndpointReturns403() throws Exception {
        mockMvc.perform(get("/api/v1/security-probe/supervisor")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void supervisorTokenOnSupervisorEndpointReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/security-probe/supervisor")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("supervisor-ok"));
    }

    @Test
    void legacyDevLoginEndpointIsGone() throws Exception {
        mockMvc.perform(post("/dev/login")
                        .contentType("application/json")
                        .content("""
                                {"username":"operateur1","role":"OPS"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
