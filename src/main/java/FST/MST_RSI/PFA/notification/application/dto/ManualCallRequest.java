package FST.MST_RSI.PFA.notification.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ManualCallRequest(
        @NotNull UUID personId,
        String alertId,
        /** SIP extension the supervisor is connected on (e.g. "9001"). Null = TTS-only fallback. */
        String supervisorExtension
) {
}
