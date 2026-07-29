package FST.MST_RSI.PFA.security.api.rest;

import FST.MST_RSI.PFA.security.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DevLoginRequest(
        @NotBlank String username,
        @NotNull Role role
) {
}
