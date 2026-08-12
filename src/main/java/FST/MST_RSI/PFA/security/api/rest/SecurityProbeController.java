package FST.MST_RSI.PFA.security.api.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security-probe")
public class SecurityProbeController {

    @GetMapping("/ops")
    @PreAuthorize("hasRole('OPS')")
    public String opsEndpoint() {
        return "ops-ok";
    }

    @GetMapping("/supervisor")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public String supervisorEndpoint() {
        return "supervisor-ok";
    }
}
