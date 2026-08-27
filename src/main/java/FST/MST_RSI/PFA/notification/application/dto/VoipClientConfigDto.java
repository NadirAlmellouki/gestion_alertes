package FST.MST_RSI.PFA.notification.application.dto;

import java.util.List;

public record VoipClientConfigDto(
        boolean enabled,
        String provider,
        String websocketUrl,
        String sipDomain,
        String sipPassword,
        List<String> extensions,
        /** Fixed SIP extension assigned to the supervisor browser phone. */
        String supervisorExtension
) {
}
