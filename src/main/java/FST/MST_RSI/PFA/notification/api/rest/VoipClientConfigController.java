package FST.MST_RSI.PFA.notification.api.rest;

import FST.MST_RSI.PFA.notification.application.dto.VoipClientConfigDto;
import FST.MST_RSI.PFA.notification.infrastructure.config.VoipNotificationProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/voip")
public class VoipClientConfigController {

    private final VoipNotificationProperties properties;

    public VoipClientConfigController(VoipNotificationProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/client-config")
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public VoipClientConfigDto clientConfig() {
        return new VoipClientConfigDto(
                properties.isEnabled(),
                properties.getProvider(),
                properties.getWsUrl(),
                properties.getSipDomain(),
                properties.getSipPassword(),
                List.of("1001", "1002", "1003", "1004"),
                "9001"  // Supervisor's dedicated WebRTC extension
        );
    }
}
