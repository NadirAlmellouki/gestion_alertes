package FST.MST_RSI.PFA.notification.api.rest;

import FST.MST_RSI.PFA.notification.application.dto.ManualCallRequest;
import FST.MST_RSI.PFA.notification.application.dto.ManualCallResult;
import FST.MST_RSI.PFA.notification.application.dto.VoiceCallSessionStatusDto;
import FST.MST_RSI.PFA.notification.application.service.LiveManualCallTracker;
import FST.MST_RSI.PFA.notification.application.usecase.PlaceManualCallUseCase;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/supervisor/manual-calls")
public class ManualCallController {

    private final PlaceManualCallUseCase placeManualCallUseCase;
    private final LiveManualCallTracker liveManualCallTracker;

    public ManualCallController(
            PlaceManualCallUseCase placeManualCallUseCase,
            LiveManualCallTracker liveManualCallTracker
    ) {
        this.placeManualCallUseCase = placeManualCallUseCase;
        this.liveManualCallTracker = liveManualCallTracker;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ManualCallResult place(@Valid @RequestBody ManualCallRequest request) {
        return placeManualCallUseCase.execute(request);
    }

    @PostMapping("/hangup")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public void hangup(
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(required = false) String channelId
    ) {
        placeManualCallUseCase.hangup(sessionId, channelId);
    }

    @GetMapping("/{sessionId}")
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public VoiceCallSessionStatusDto status(@PathVariable UUID sessionId) {
        return placeManualCallUseCase.status(sessionId);
    }

    @GetMapping(path = "/{sessionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public SseEmitter events(@PathVariable UUID sessionId) {
        return liveManualCallTracker.subscribe(sessionId);
    }
}
