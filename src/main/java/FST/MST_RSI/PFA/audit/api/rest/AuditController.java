package FST.MST_RSI.PFA.audit.api.rest;

import FST.MST_RSI.PFA.audit.application.dto.AuditLogDto;
import FST.MST_RSI.PFA.audit.application.dto.AuditTimelineEntryDto;
import FST.MST_RSI.PFA.audit.application.usecase.GetAlertAuditTimelineUseCase;
import FST.MST_RSI.PFA.audit.application.usecase.ListAuditLogsUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final ListAuditLogsUseCase listAuditLogsUseCase;
    private final GetAlertAuditTimelineUseCase getAlertAuditTimelineUseCase;

    public AuditController(
            ListAuditLogsUseCase listAuditLogsUseCase,
            GetAlertAuditTimelineUseCase getAlertAuditTimelineUseCase
    ) {
        this.listAuditLogsUseCase = listAuditLogsUseCase;
        this.getAlertAuditTimelineUseCase = getAlertAuditTimelineUseCase;
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public List<AuditLogDto> listLogs(
            @RequestParam(required = false) UUID alertId,
            @RequestParam(required = false) String action,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return listAuditLogsUseCase.execute(alertId, action, from, to, page, size);
    }

    @GetMapping("/alerts/{alertId}/timeline")
    @PreAuthorize("hasAnyRole('OPS','SUPERVISOR')")
    public List<AuditTimelineEntryDto> alertTimeline(@PathVariable String alertId) {
        return getAlertAuditTimelineUseCase.execute(alertId);
    }
}
