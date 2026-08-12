package FST.MST_RSI.PFA.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Test-only decoder so the context starts without a live Keycloak instance.
 * Production / dev rely on {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}.
 */
@Configuration
@Profile("test")
public class TestJwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${app.security.test-jwt-secret:test-secret-key-at-least-256-bits-long-for-hs256-algorithm}")
            String secret
    ) {
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
