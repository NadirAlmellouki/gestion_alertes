package FST.MST_RSI.PFA.security.api.rest;

import FST.MST_RSI.PFA.security.infrastructure.JwtTokenService;
import FST.MST_RSI.PFA.security.infrastructure.persistence.UserRoleMappingEntity;
import FST.MST_RSI.PFA.security.infrastructure.persistence.UserRoleMappingRepository;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dev")
@Profile("dev")
public class DevLoginController {

    private final JwtTokenService jwtTokenService;
    private final UserRoleMappingRepository userRoleMappingRepository;

    public DevLoginController(
            JwtTokenService jwtTokenService,
            UserRoleMappingRepository userRoleMappingRepository
    ) {
        this.jwtTokenService = jwtTokenService;
        this.userRoleMappingRepository = userRoleMappingRepository;
    }

    @PostMapping("/login")
    public DevLoginResponse login(@Valid @RequestBody DevLoginRequest request) {
        userRoleMappingRepository.findByUsername(request.username())
                .ifPresentOrElse(
                        existing -> {
                            existing.setRole(request.role());
                            userRoleMappingRepository.save(existing);
                        },
                        () -> userRoleMappingRepository.save(
                                new UserRoleMappingEntity(request.username(), request.role())
                        )
                );

        String token = jwtTokenService.generateToken(request.username(), request.role());
        return new DevLoginResponse(token, request.role(), request.username());
    }
}
