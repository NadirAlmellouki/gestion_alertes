package FST.MST_RSI.PFA.security.application;

import FST.MST_RSI.PFA.security.config.SecurityClaimsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationUserProfileServiceTest {

    private final ApplicationUserProfileService service =
            new ApplicationUserProfileService(new SecurityClaimsProperties("given_name", "family_name"));

    @Test
    void mapsJwtClaimsToProfile() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("given_name", "Nadir")
                .claim("family_name", "Almellouki")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_OPS"))
        );

        var profile = service.fromAuthentication(auth);

        assertThat(profile.firstName()).isEqualTo("Nadir");
        assertThat(profile.lastName()).isEqualTo("Almellouki");
        assertThat(profile.displayRole()).isEqualTo("OPS");
    }

    @Test
    void resolvesSupervisorFromRealmRolesInJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("given_name", "Nadia")
                .claim("family_name", "Benchekroun")
                .claim("realm_access", Map.of("roles", List.of("SUPERVISOR", "offline_access")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))
        );

        assertThat(service.fromAuthentication(auth).displayRole()).isEqualTo("SUPERVISOR");
    }
}
