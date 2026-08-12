package FST.MST_RSI.PFA.alerting.api.rest;

import FST.MST_RSI.PFA.alerting.application.dto.AlertDto;
import FST.MST_RSI.PFA.alerting.application.dto.AlertSummaryDto;
import FST.MST_RSI.PFA.alerting.application.usecase.GetAlertDetailUseCase;
import FST.MST_RSI.PFA.alerting.application.usecase.ListAlertHistoryUseCase;
import FST.MST_RSI.PFA.alerting.application.usecase.ListRecentAlertsUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final ListRecentAlertsUseCase listRecentAlertsUseCase;
    private final ListAlertHistoryUseCase listAlertHistoryUseCase;
    private final GetAlertDetailUseCase getAlertDetailUseCase;

    public AlertController(
            ListRecentAlertsUseCase listRecentAlertsUseCase,
            ListAlertHistoryUseCase listAlertHistoryUseCase,
            GetAlertDetailUseCase getAlertDetailUseCase
    ) {
        this.listRecentAlertsUseCase = listRecentAlertsUseCase;
        this.listAlertHistoryUseCase = listAlertHistoryUseCase;
        this.getAlertDetailUseCase = getAlertDetailUseCase;
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public List<AlertSummaryDto> listRecent() {
        return listRecentAlertsUseCase.execute();
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public List<AlertSummaryDto> listHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return listAlertHistoryUseCase.execute(from, to, page, size);
    }

    @GetMapping("/{alertId}")
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public AlertDto getDetail(@PathVariable String alertId) {
        return getAlertDetailUseCase.execute(alertId);
    }
}
