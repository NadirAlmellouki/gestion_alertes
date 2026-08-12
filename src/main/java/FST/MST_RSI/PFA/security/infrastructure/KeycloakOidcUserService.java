package FST.MST_RSI.PFA.security.infrastructure;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps Keycloak OIDC tokens to Spring authorities using the same rules as the JWT resource server.
 */
@Component
public class KeycloakOidcUserService extends OidcUserService {

    private final KeycloakAuthoritiesExtractor authoritiesExtractor;

    public KeycloakOidcUserService(KeycloakAuthoritiesExtractor authoritiesExtractor) {
        this.authoritiesExtractor = authoritiesExtractor;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser delegateUser = super.loadUser(userRequest);
        Map<String, Object> mergedClaims = new HashMap<>(delegateUser.getClaims());
        Collection<GrantedAuthority> authorities = authoritiesExtractor.fromClaims(mergedClaims);
        return new DefaultOidcUser(
                authorities,
                delegateUser.getIdToken(),
                delegateUser.getUserInfo(),
                "preferred_username"
        );
    }
}
