package FST.MST_RSI.PFA.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.claims")
public record SecurityClaimsProperties(
        String firstName,
        String lastName
) {
    public SecurityClaimsProperties {
        if (firstName == null || firstName.isBlank()) {
            firstName = "given_name";
        }
        if (lastName == null || lastName.isBlank()) {
            lastName = "family_name";
        }
    }
}
