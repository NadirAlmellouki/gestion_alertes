package FST.MST_RSI.PFA.security.api.rest;

import FST.MST_RSI.PFA.security.domain.Role;

public record DevLoginResponse(
        String accessToken,
        String tokenType,
        Role role,
        String username
) {
    public DevLoginResponse(String accessToken, Role role, String username) {
        this(accessToken, "Bearer", role, username);
    }
}
