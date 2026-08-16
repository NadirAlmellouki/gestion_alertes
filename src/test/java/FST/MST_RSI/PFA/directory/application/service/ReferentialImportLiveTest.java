package FST.MST_RSI.PFA.directory.application.service;

import FST.MST_RSI.PFA.directory.application.dto.ReferentialImportReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live import against the local PostgreSQL container (not H2).
 * Run: mvn test -Dtest=ReferentialImportLiveTest
 */
@Tag("live")
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/alertops",
        "spring.datasource.username=alertops",
        "spring.datasource.password=alertops",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "app.referential.excel-path=file:info/referentiel-applicatif-complet-2026-08-04.xlsx",
        "app.business-context.source=postgres"
})
class ReferentialImportLiveTest {

    private static final Logger log = LoggerFactory.getLogger(ReferentialImportLiveTest.class);

    @Autowired
    private ReferentialImportService referentialImportService;

    @Test
    void importReferentialIntoPostgres() {
        ReferentialImportReport report = referentialImportService.importReferential();

        log.info("""
                Referential import finished:
                  entities={}, poles={}, domains={}
                  solutions={} (active={}, inactive={})
                  persons inserted={}, updated={}, rejected={}
                  assignments={}, assignmentsRejected={}, rowsRejected={}
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
        report.rejectionSamples().forEach(sample -> log.warn("Rejection sample: {}", sample));

        assertThat(report.entitiesImported()).isGreaterThan(0);
        assertThat(report.solutionsImported()).isGreaterThan(400);
        assertThat(report.personsImported() + report.personsUpdated()).isGreaterThan(300);
        assertThat(report.personsRejected()).isBetween(100, 130);
    }
}
