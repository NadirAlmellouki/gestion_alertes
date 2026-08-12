package FST.MST_RSI.PFA.directory.api.rest;

import FST.MST_RSI.PFA.directory.application.dto.ReferentialImportReport;
import FST.MST_RSI.PFA.directory.application.service.ReferentialImportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/referential")
public class ReferentialImportController {

    private final ReferentialImportService referentialImportService;

    public ReferentialImportController(ReferentialImportService referentialImportService) {
        this.referentialImportService = referentialImportService;
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ReferentialImportReport importReferential() {
        return referentialImportService.importReferential();
    }
}
