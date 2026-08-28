package FST.MST_RSI.PFA.dashboard.api.rest;

import FST.MST_RSI.PFA.dashboard.application.dto.DashboardAdminsDto;
import FST.MST_RSI.PFA.dashboard.application.dto.DashboardOverviewDto;
import FST.MST_RSI.PFA.dashboard.application.dto.DashboardVoipDto;
import FST.MST_RSI.PFA.dashboard.application.usecase.GetDashboardAdminsUseCase;
import FST.MST_RSI.PFA.dashboard.application.usecase.GetDashboardOverviewUseCase;
import FST.MST_RSI.PFA.dashboard.application.usecase.GetDashboardVoipUseCase;
import FST.MST_RSI.PFA.notification.application.dto.ManualCallRequest;
import FST.MST_RSI.PFA.notification.application.dto.ManualCallResult;
import FST.MST_RSI.PFA.notification.application.usecase.PlaceManualCallUseCase;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private static final long DEFAULT_WINDOW_DAYS = 7;

    private final GetDashboardOverviewUseCase getDashboardOverviewUseCase;
    private final GetDashboardVoipUseCase getDashboardVoipUseCase;
    private final GetDashboardAdminsUseCase getDashboardAdminsUseCase;
    private final PlaceManualCallUseCase placeManualCallUseCase;

    public DashboardController(
            GetDashboardOverviewUseCase getDashboardOverviewUseCase,
            GetDashboardVoipUseCase getDashboardVoipUseCase,
            GetDashboardAdminsUseCase getDashboardAdminsUseCase,
            PlaceManualCallUseCase placeManualCallUseCase
    ) {
        this.getDashboardOverviewUseCase = getDashboardOverviewUseCase;
        this.getDashboardVoipUseCase = getDashboardVoipUseCase;
        this.getDashboardAdminsUseCase = getDashboardAdminsUseCase;
        this.placeManualCallUseCase = placeManualCallUseCase;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public DashboardOverviewDto overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        InstantRange range = resolveRange(from, to);
        return getDashboardOverviewUseCase.execute(range.from(), range.to());
    }

    @GetMapping("/voip")
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public DashboardVoipDto voip(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        InstantRange range = resolveRange(from, to);
        return getDashboardVoipUseCase.execute(range.from(), range.to());
    }

    @GetMapping("/admins")
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public DashboardAdminsDto admins(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        InstantRange range = resolveRange(from, to);
        return getDashboardAdminsUseCase.execute(range.from(), range.to());
    }

    @PostMapping("/manual-calls")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ManualCallResult placeManualCall(@Valid @RequestBody ManualCallRequest request) {
        return placeManualCallUseCase.execute(request);
    }

    @PostMapping("/manual-calls/hangup")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public void hangupManualCall(
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(required = false) String channelId
    ) {
        placeManualCallUseCase.hangup(sessionId, channelId);
    }

    private InstantRange resolveRange(Instant from, Instant to) {
        Instant end = to != null ? to : Instant.now();
        Instant start = from != null ? from : end.minus(DEFAULT_WINDOW_DAYS, ChronoUnit.DAYS);
        return new InstantRange(start, end);
    }

    private record InstantRange(Instant from, Instant to) {
    }
}
