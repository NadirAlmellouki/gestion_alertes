package FST.MST_RSI.PFA.security.infrastructure;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extracts Keycloak realm / client roles as Spring {@code ROLE_*} authorities.
 * Shared by JWT resource-server and OIDC browser login.
 */
@Component
public class KeycloakAuthoritiesExtractor {

    private static final String REALM_ACCESS = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";

    public Collection<GrantedAuthority> fromClaims(Map<String, Object> claims) {
        List<String> roles = new ArrayList<>();
        collectRoles(claims, roles);

        Object singleRole = claims.get("role");
        if (singleRole instanceof String roleStr && !roleStr.isBlank()) {
            roles.add(roleStr);
        }

        return roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    public Collection<GrantedAuthority> fromJwt(Jwt jwt) {
        return fromClaims(jwt.getClaims());
    }

    @SuppressWarnings("unchecked")
    private void collectRoles(Map<String, Object> claims, List<String> roles) {
        Object realmAccess = claims.get(REALM_ACCESS);
        if (realmAccess instanceof Map<?, ?> realmMap && realmMap.get(ROLES) instanceof Collection<?> realmRoles) {
            realmRoles.forEach(role -> roles.add(String.valueOf(role)));
        }

        Object resourceAccess = claims.get(RESOURCE_ACCESS);
        if (resourceAccess instanceof Map<?, ?> resourceMap) {
            for (Object clientAccess : resourceMap.values()) {
                if (clientAccess instanceof Map<?, ?> clientMap
                        && clientMap.get(ROLES) instanceof Collection<?> clientRoles) {
                    clientRoles.forEach(role -> roles.add(String.valueOf(role)));
                }
            }
        }
    }
}
