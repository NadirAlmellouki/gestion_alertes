package FST.MST_RSI.PFA.security.api.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/security-probe")
public class SecurityProbeController {

    @GetMapping("/operator")
    @PreAuthorize("hasRole('OPERATEUR')")
    public String operatorEndpoint() {
        return "operator-ok";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public String adminEndpoint() {
        return "admin-ok";
    }
}
