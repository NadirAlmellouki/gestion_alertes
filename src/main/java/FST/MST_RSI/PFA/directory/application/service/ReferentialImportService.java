package FST.MST_RSI.PFA.directory.application.service;

import FST.MST_RSI.PFA.directory.application.dto.ReferentialImportReport;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.OrganizationalUnitEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.OrganizationalUnitRepository;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonRepository;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.SolutionAttributeEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.SolutionAttributeRepository;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.UnitAdminAssignmentEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.UnitAdminAssignmentRepository;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReferentialImportService {

    private static final Logger log = LoggerFactory.getLogger(ReferentialImportService.class);
    private static final DataFormatter FORMATTER = new DataFormatter(Locale.ROOT);
    private static final String MANAGER_ROLE = "MANAGER";
    private static final Map<String, Integer> ROLE_COLUMNS = roleColumns();

    private final OrganizationalUnitRepository unitRepository;
    private final PersonRepository personRepository;
    private final SolutionAttributeRepository solutionAttributeRepository;
    private final UnitAdminAssignmentRepository assignmentRepository;
    private final Resource excelResource;

    public ReferentialImportService(
            OrganizationalUnitRepository unitRepository,
            PersonRepository personRepository,
            SolutionAttributeRepository solutionAttributeRepository,
            UnitAdminAssignmentRepository assignmentRepository,
            @Value("${app.referential.excel-path:file:./info/referentiel-applicatif-complet-2026-08-04.xlsx}")
            Resource excelResource
    ) {
        this.unitRepository = unitRepository;
        this.personRepository = personRepository;
        this.solutionAttributeRepository = solutionAttributeRepository;
        this.assignmentRepository = assignmentRepository;
        this.excelResource = excelResource;
    }

    @Transactional
    public ReferentialImportReport importReferential() {
        ReferentialImportReport.Builder report = ReferentialImportReport.builder();
        Instant now = Instant.now();
        Map<String, UUID> entityIds = new HashMap<>();
        Map<String, UUID> poleIds = new HashMap<>();
        Map<String, UUID> domainIds = new HashMap<>();

        try (InputStream in = excelResource.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            importEntities(workbook.getSheet("Entités"), entityIds, report, now);
            importPoles(workbook.getSheet("Pôles"), entityIds, poleIds, report, now);
            importDomains(workbook.getSheet("Domaines"), poleIds, domainIds, report, now);
            importPersons(workbook.getSheet("Personnes"), report, now);
            importSolutions(workbook.getSheet("Solutions"), domainIds, report, now);
        } catch (Exception ex) {
            log.error("Referential import failed", ex);
            throw new IllegalStateException("Referential import failed: " + ex.getMessage(), ex);
        }

        ReferentialImportReport result = report.build("Import terminé avec succès");
        log.info("Referential import: entities={}, poles={}, domains={}, solutions={} (active={}, inactive={}), persons={}/updated={}, rejected persons={}, assignments={}, rejected={}",
                result.entitiesImported(), result.polesImported(), result.domainsImported(),
                result.solutionsImported(), result.solutionsActiveImported(), result.solutionsInactiveImported(),
                result.personsImported(), result.personsUpdated(), result.personsRejected(),
                result.assignmentsImported(), result.rowsRejected());
        return result;
    }

    private void importEntities(
            Sheet sheet,
            Map<String, UUID> entityIds,
            ReferentialImportReport.Builder report,
            Instant now
    ) {
        int count = 0;
        for (Row row : sheet) {
            if (row.getRowNum() < 4) {
                continue;
            }
            String sigle = cell(row, 0);
            String name = cell(row, 1);
            String managerEmail = cell(row, 2);
            String subsidiary = cell(row, 3);
            String active = cell(row, 4);
            if (isBlank(name)) {
                continue;
            }
            boolean isActive = !"Non".equalsIgnoreCase(active);
            OrganizationalUnitEntity unit = upsertUnit(
                    "ENTITY", name, null, codeOrSlug(sigle, name),
                    "Oui".equalsIgnoreCase(subsidiary), isActive, now
            );
            entityIds.put(normalizeKey(name), unit.getId());
            assignManager(unit.getId(), managerEmail, report, now, "Entité " + name);
            count++;
        }
        report.entitiesImported(count);
    }

    private void importPoles(
            Sheet sheet,
            Map<String, UUID> entityIds,
            Map<String, UUID> poleIds,
            ReferentialImportReport.Builder report,
            Instant now
    ) {
        int count = 0;
        for (Row row : sheet) {
            if (row.getRowNum() < 4) {
                continue;
            }
            String poleName = cell(row, 0);
            String entityName = cell(row, 1);
            String managerEmail = cell(row, 2);
            if (isBlank(poleName) || isBlank(entityName)) {
                report.rowsRejected(report.rowsRejected() + 1);
                report.addRejectionSample("Pôle sans nom ou entité (ligne " + (row.getRowNum() + 1) + ")");
                continue;
            }
            UUID parentId = entityIds.get(normalizeKey(entityName));
            if (parentId == null) {
                report.rowsRejected(report.rowsRejected() + 1);
                report.addRejectionSample("Pôle '" + poleName + "' : entité '" + entityName + "' introuvable");
                continue;
            }
            OrganizationalUnitEntity unit = upsertUnit(
                    "POLE", poleName, parentId, codeOrSlug(null, poleName + "-" + entityName),
                    false, true, now
            );
            poleIds.put(poleKey(poleName, entityName), unit.getId());
            assignManager(unit.getId(), managerEmail, report, now, "Pôle " + poleName);
            count++;
        }
        report.polesImported(count);
    }

    private void importDomains(
            Sheet sheet,
            Map<String, UUID> poleIds,
            Map<String, UUID> domainIds,
            ReferentialImportReport.Builder report,
            Instant now
    ) {
        int count = 0;
        for (Row row : sheet) {
            if (row.getRowNum() < 4) {
                continue;
            }
            String domainName = cell(row, 0);
            String poleName = cell(row, 1);
            String managerEmail = cell(row, 2);
            if (isBlank(domainName)) {
                continue;
            }
            UUID parentId = findPoleId(poleIds, poleName);
            if (parentId == null) {
                report.rowsRejected(report.rowsRejected() + 1);
                report.addRejectionSample("Domaine '" + domainName + "' : pôle '" + poleName + "' introuvable");
                continue;
            }
            OrganizationalUnitEntity unit = upsertUnit(
                    "DOMAIN", domainName, parentId, codeOrSlug(null, domainName + "-" + poleName),
                    false, true, now
            );
            domainIds.put(domainKey(domainName, poleName), unit.getId());
            assignManager(unit.getId(), managerEmail, report, now, "Domaine " + domainName);
            count++;
        }
        report.domainsImported(count);
    }

    private void importPersons(Sheet sheet, ReferentialImportReport.Builder report, Instant now) {
        int inserted = 0;
        int updated = 0;
        int rejected = 0;
        for (Row row : sheet) {
            if (row.getRowNum() < 4) {
                continue;
            }
            ReferentialPersonNormalizer.ParsedPerson parsed = ReferentialPersonNormalizer.fromPersonSheet(
                    cell(row, 0), cell(row, 1), cell(row, 2)
            );
            if (parsed == null) {
                rejected++;
                report.addRejectionSample("Personne rejetée (ligne " + (row.getRowNum() + 1) + ") : e-mail invalide ou absent");
                continue;
            }
            UpsertResult result = upsertPerson(parsed.firstName(), parsed.lastName(), parsed.fullName(), parsed.email(), now);
            if (result.created()) {
                inserted++;
            } else {
                updated++;
            }
        }
        report.personsImported(inserted);
        report.personsUpdated(updated);
        report.personsRejected(rejected);
    }

    private void importSolutions(
            Sheet sheet,
            Map<String, UUID> domainIds,
            ReferentialImportReport.Builder report,
            Instant now
    ) {
        int count = 0;
        int activeCount = 0;
        int inactiveCount = 0;
        int assignments = report.assignmentsImported();
        int assignmentRejected = report.assignmentsRejected();

        for (Row row : sheet) {
            if (row.getRowNum() < 4) {
                continue;
            }
            String solutionName = cell(row, 0);
            String poleName = cell(row, 3);
            String domainName = cell(row, 4);
            String activeFlag = cell(row, 9);
            if (isBlank(solutionName)) {
                continue;
            }
            boolean isActive = "Oui".equalsIgnoreCase(activeFlag);

            UUID parentId = findDomainId(domainIds, domainName, poleName);
            if (parentId == null) {
                report.rowsRejected(report.rowsRejected() + 1);
                report.addRejectionSample("Solution '" + solutionName + "' : hiérarchie domaine/pôle introuvable");
                continue;
            }

            OrganizationalUnitEntity solutionUnit = upsertUnit(
                    "SOLUTION", solutionName, parentId, codeOrSlug(cell(row, 1), solutionName),
                    false, isActive, now
            );

            SolutionAttributeEntity attributes = solutionAttributeRepository.findById(solutionUnit.getId())
                    .orElseGet(() -> SolutionAttributeEntity.create(
                            solutionUnit.getId(), null, null, null, null, null, null, isActive
                    ));
            attributes.setSolutionType(truncate(emptyToNull(cell(row, 5)), 100));
            attributes.setPsi(truncate(emptyToNull(cell(row, 6)), 100));
            attributes.setFunctionalDescription(emptyToNull(cell(row, 7)));
            attributes.setTargetScope(truncate(emptyToNull(cell(row, 8)), 150));
            attributes.setServiceType(truncate(emptyToNull(cell(row, 21)), 100));
            attributes.setTenant(truncate(emptyToNull(cell(row, 22)), 100));
            attributes.setActive(isActive);
            solutionAttributeRepository.save(attributes);

            count++;
            if (isActive) {
                activeCount++;
            } else {
                inactiveCount++;
            }

            for (Map.Entry<String, Integer> roleEntry : ROLE_COLUMNS.entrySet()) {
                String emailRaw = cell(row, roleEntry.getValue());
                ReferentialPersonNormalizer.ParsedPerson parsed = ReferentialPersonNormalizer.fromEmailOnly(emailRaw);
                if (parsed == null) {
                    continue;
                }
                Optional<PersonEntity> personOpt = personRepository.findByEmailIgnoreCase(parsed.email());
                if (personOpt.isEmpty()) {
                    assignmentRejected++;
                    report.addRejectionSample("Affectation " + roleEntry.getKey() + " pour '" + solutionName
                            + "' : personne '" + emailRaw + "' introuvable");
                    continue;
                }
                if (upsertAssignment(solutionUnit.getId(), personOpt.get().getId(), roleEntry.getKey(), now)) {
                    assignments++;
                }
            }
        }

        report.solutionsImported(count);
        report.solutionsActiveImported(activeCount);
        report.solutionsInactiveImported(inactiveCount);
        report.assignmentsImported(assignments);
        report.assignmentsRejected(assignmentRejected);
    }

    private void assignManager(
            UUID unitId,
            String managerEmailRaw,
            ReferentialImportReport.Builder report,
            Instant now,
            String context
    ) {
        ReferentialPersonNormalizer.ParsedPerson parsed = ReferentialPersonNormalizer.fromEmailOnly(managerEmailRaw);
        if (parsed == null) {
            return;
        }
        Optional<PersonEntity> existing = personRepository.findByEmailIgnoreCase(parsed.email());
        PersonEntity person;
        if (existing.isPresent()) {
            person = existing.get();
        } else {
            UpsertResult created = upsertPerson(parsed.firstName(), parsed.lastName(), parsed.fullName(), parsed.email(), now);
            person = created.person();
            if (created.created()) {
                report.personsImported(report.personsImported() + 1);
            } else {
                report.personsUpdated(report.personsUpdated() + 1);
            }
        }
        if (upsertAssignment(unitId, person.getId(), MANAGER_ROLE, now)) {
            report.assignmentsImported(report.assignmentsImported() + 1);
        }
    }

    private OrganizationalUnitEntity upsertUnit(
            String unitType,
            String name,
            UUID parentId,
            String code,
            boolean subsidiary,
            boolean active,
            Instant now
    ) {
        String safeName = truncate(name, 255);
        String safeCode = ensureUniqueCode(truncateCode(code), null);
        Optional<OrganizationalUnitEntity> existing = parentId == null
                ? unitRepository.findByUnitTypeAndNameAndParentUnitIdIsNull(unitType, safeName)
                : unitRepository.findByUnitTypeAndNameAndParentUnitId(unitType, safeName, parentId);

        if (existing.isPresent()) {
            OrganizationalUnitEntity entity = existing.get();
            entity.setCode(ensureUniqueCode(safeCode, entity.getId()));
            entity.setActive(active);
            entity.setSubsidiary(subsidiary);
            entity.touch(now);
            return unitRepository.save(entity);
        }

        return unitRepository.save(OrganizationalUnitEntity.create(
                UUID.randomUUID(), safeCode, safeName, unitType, parentId, subsidiary, active, now
        ));
    }

    private UpsertResult upsertPerson(
            String firstName,
            String lastName,
            String fullName,
            String email,
            Instant now
    ) {
        Optional<PersonEntity> existing = personRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            PersonEntity person = existing.get();
            if (firstName != null) {
                person.setFirstName(firstName);
            }
            if (lastName != null) {
                person.setLastName(lastName);
            }
            person.setFullName(fullName);
            person.touch(now);
            return new UpsertResult(personRepository.save(person), false);
        }
        PersonEntity created = personRepository.save(PersonEntity.create(
                UUID.randomUUID(), firstName, lastName, fullName, email, true, now
        ));
        return new UpsertResult(created, true);
    }

    private boolean upsertAssignment(UUID unitId, UUID personId, String role, Instant now) {
        if (assignmentRepository.findByUnitIdAndPersonIdAndRole(unitId, personId, role).isPresent()) {
            return false;
        }
        assignmentRepository.save(UnitAdminAssignmentEntity.create(
                UUID.randomUUID(), unitId, personId, role, false, now
        ));
        return true;
    }

    private static UUID findPoleId(Map<String, UUID> poleIds, String poleName) {
        if (isBlank(poleName)) {
            return null;
        }
        String prefix = normalizeKey(poleName) + "|";
        return poleIds.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static UUID findDomainId(Map<String, UUID> domainIds, String domainName, String poleName) {
        if (isBlank(domainName)) {
            return null;
        }
        UUID exact = domainIds.get(domainKey(domainName, poleName));
        if (exact != null) {
            return exact;
        }
        return domainIds.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(normalizeKey(domainName) + "|"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static String poleKey(String poleName, String entityName) {
        return normalizeKey(poleName) + "|" + normalizeKey(entityName);
    }

    private static String domainKey(String domainName, String poleName) {
        return normalizeKey(domainName) + "|" + normalizeKey(poleName);
    }

    private static String cell(Row row, int index) {
        if (row.getCell(index) == null) {
            return null;
        }
        String value = FORMATTER.formatCellValue(row.getCell(index));
        return value == null ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String emptyToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private static String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String codeOrSlug(String preferred, String fallback) {
        String base = !isBlank(preferred) ? preferred : fallback;
        if (isBlank(base)) {
            return "UNKNOWN";
        }
        String slug = base.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (slug.isEmpty()) {
            slug = "UNIT";
        }
        return truncateCode(slug);
    }

    private String ensureUniqueCode(String code, UUID excludeId) {
        String candidate = code;
        int suffix = 1;
        while (isCodeTaken(candidate, excludeId)) {
            String suffixToken = "_" + suffix++;
            int maxBase = 100 - suffixToken.length();
            String base = code.length() > maxBase ? code.substring(0, maxBase) : code;
            candidate = base + suffixToken;
        }
        return candidate;
    }

    private boolean isCodeTaken(String code, UUID excludeId) {
        return unitRepository.findByCode(code)
                .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                .isPresent();
    }

    private static String truncateCode(String slug) {
        if (slug.length() <= 100) {
            return slug;
        }
        String suffix = String.format("%08X", slug.hashCode() & 0xFFFFFFFFL);
        return slug.substring(0, 91) + "_" + suffix;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static Map<String, Integer> roleColumns() {
        Map<String, Integer> columns = new LinkedHashMap<>();
        columns.put("TAM", 10);
        columns.put("TECHNICAL_ADMIN", 11);
        columns.put("FUNCTIONAL_ADMIN", 12);
        columns.put("RECETTE", 13);
        columns.put("PROD_ARCHITECT", 14);
        columns.put("DSA_ARCHITECT", 15);
        columns.put("PRODUCTION_PROJECT_MANAGER", 16);
        columns.put("DBA", 17);
        columns.put("INFRA_ADMIN", 18);
        columns.put("APP_SECURITY_OFFICER", 19);
        columns.put("ENGINEERING_SECURITY_OFFICER", 20);
        return columns;
    }

    private record UpsertResult(PersonEntity person, boolean created) {
    }
}
