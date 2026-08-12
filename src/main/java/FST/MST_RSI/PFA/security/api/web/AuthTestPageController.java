package FST.MST_RSI.PFA.security.api.web;

import FST.MST_RSI.PFA.security.application.ApplicationUserProfileService;
import FST.MST_RSI.PFA.security.domain.AuthenticatedUserProfile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
public class AuthTestPageController {

    private final ApplicationUserProfileService userProfileService;

    public AuthTestPageController(ApplicationUserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping(value = "/test", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> testPage(Authentication authentication) {
        AuthenticatedUserProfile profile = userProfileService.fromAuthentication(authentication);
        String body = """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8"/>
                  <title>AlertOps — test authentification</title>
                </head>
                <body>
                  <p>Je suis %s %s et je suis un %s</p>
                </body>
                </html>
                """.formatted(
                safe(profile.lastName()),
                safe(profile.firstName()),
                profile.displayRole()
        );
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(body);
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return escapeHtml(value);
    }

    private static String escapeHtml(String raw) {
        return raw
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
