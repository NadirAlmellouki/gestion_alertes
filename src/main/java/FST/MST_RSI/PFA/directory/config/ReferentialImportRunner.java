package FST.MST_RSI.PFA.directory.config;

import FST.MST_RSI.PFA.directory.application.dto.ReferentialImportReport;
import FST.MST_RSI.PFA.directory.application.service.ReferentialImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.referential", name = "import-on-startup", havingValue = "true")
public class ReferentialImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReferentialImportRunner.class);

    private final ReferentialImportService referentialImportService;

    public ReferentialImportRunner(ReferentialImportService referentialImportService) {
        this.referentialImportService = referentialImportService;
    }

    @Override
    public void run(ApplicationArguments args) {
        ReferentialImportReport report = referentialImportService.importReferential();
        log.info("""
                Referential import finished:
                  entities={}, poles={}, domains={}
                  solutions={} (active={}, inactive={})
                  persons inserted={}, updated={}, rejected={}
                  assignments={}, assignmentsRejected={}, totalRejected={}
                """,
                report.entitiesImported(),
                report.polesImported(),
                report.domainsImported(),
                report.solutionsImported(),
                report.solutionsActiveImported(),
                report.solutionsInactiveImported(),
                report.personsImported(),
                report.personsUpdated(),
                report.personsRejected(),
                report.assignmentsImported(),
                report.assignmentsRejected(),
                report.rowsRejected()
        );
        report.rejectionSamples().forEach(sample -> log.warn("Import rejection sample: {}", sample));
    }
}
