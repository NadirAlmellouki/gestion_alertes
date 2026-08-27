package FST.MST_RSI.PFA.notification.api.rest;

import FST.MST_RSI.PFA.notification.application.dto.ManualCallRequest;
import FST.MST_RSI.PFA.notification.application.dto.ManualCallResult;
import FST.MST_RSI.PFA.notification.application.usecase.PlaceManualCallUseCase;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/supervisor/manual-calls")
public class ManualCallController {

    private final PlaceManualCallUseCase placeManualCallUseCase;

    public ManualCallController(PlaceManualCallUseCase placeManualCallUseCase) {
        this.placeManualCallUseCase = placeManualCallUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ManualCallResult place(@Valid @RequestBody ManualCallRequest request) {
        return placeManualCallUseCase.execute(request);
    }
}
