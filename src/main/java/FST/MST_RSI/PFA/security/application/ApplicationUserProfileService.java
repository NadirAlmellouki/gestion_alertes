package FST.MST_RSI.PFA.security.application;

import FST.MST_RSI.PFA.security.config.SecurityClaimsProperties;
import FST.MST_RSI.PFA.security.domain.AuthenticatedUserProfile;
import FST.MST_RSI.PFA.security.domain.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

/**
 * Maps Keycloak / OAuth2 identity to display fields (nom, prénom, rôle applicatif).
 */
@Service
public class ApplicationUserProfileService {

    private final SecurityClaimsProperties claimsProperties;

    public ApplicationUserProfileService(SecurityClaimsProperties claimsProperties) {
        this.claimsProperties = claimsProperties;
    }

    public AuthenticatedUserProfile fromAuthentication(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            return profileFromOidc(oidcUser, authentication);
        }
        if (principal instanceof Jwt jwt) {
            return profileFromJwt(jwt, authentication);
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth && jwtAuth.getToken() != null) {
            return profileFromJwt(jwtAuth.getToken(), authentication);
        }
        throw new IllegalStateException("Unsupported authentication principal: " + principal.getClass().getName());
    }

    private AuthenticatedUserProfile profileFromOidc(OidcUser oidcUser, Authentication authentication) {
        String firstName = claimAsString(oidcUser, claimsProperties.firstName());
        String lastName = claimAsString(oidcUser, claimsProperties.lastName());
        Role role = resolveApplicationRole(authentication).orElse(null);
        return new AuthenticatedUserProfile(lastName, firstName, role);
    }

    private AuthenticatedUserProfile profileFromJwt(Jwt jwt, Authentication authentication) {
        String firstName = jwt.getClaimAsString(claimsProperties.firstName());
        String lastName = jwt.getClaimAsString(claimsProperties.lastName());
        Role role = resolveApplicationRole(authentication).orElse(null);
        return new AuthenticatedUserProfile(lastName, firstName, role);
    }

    private static String claimAsString(OidcUser user, String claimName) {
        String value = user.getClaimAsString(claimName);
        return value != null ? value : "";
    }

    private Optional<Role> resolveApplicationRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(ApplicationUserProfileService::stripRolePrefix)
                .flatMap(name -> Arrays.stream(Role.values())
                        .filter(r -> r.name().equalsIgnoreCase(name))
                        .findFirst()
                        .stream())
                .findFirst();
    }

    private static String stripRolePrefix(String authority) {
        if (authority != null && authority.startsWith("ROLE_")) {
            return authority.substring("ROLE_".length());
        }
        return authority;
    }
}
