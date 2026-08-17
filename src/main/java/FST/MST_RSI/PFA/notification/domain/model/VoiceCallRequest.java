package FST.MST_RSI.PFA.notification.domain.model;

import java.util.UUID;

public record VoiceCallRequest(
        UUID alertId,
        UUID routingExecutionId,
        UUID personId,
        String phoneNumber,
        String recipientName,
        String message,
        byte[] audioContent,
        String audioContentType,
        String correlationId
) {
}
