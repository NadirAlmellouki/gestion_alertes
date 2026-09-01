package FST.MST_RSI.PFA.alerting.application.usecase;

import FST.MST_RSI.PFA.alerting.application.dto.AlertStakeholderDto;
import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.alerting.domain.model.AlertId;
import FST.MST_RSI.PFA.alerting.domain.port.AlertRepositoryPort;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.OrganizationalUnitEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.OrganizationalUnitRepository;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonContactStateEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonContactStateRepository;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonRepository;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.UnitAdminAssignmentEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.UnitAdminAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class GetAlertStakeholdersUseCase {

    private final AlertRepositoryPort alertRepositoryPort;
    private final OrganizationalUnitRepository unitRepository;
    private final UnitAdminAssignmentRepository assignmentRepository;
    private final PersonRepository personRepository;
    private final PersonContactStateRepository contactStateRepository;

    public GetAlertStakeholdersUseCase(
            AlertRepositoryPort alertRepositoryPort,
            OrganizationalUnitRepository unitRepository,
            UnitAdminAssignmentRepository assignmentRepository,
            PersonRepository personRepository,
            PersonContactStateRepository contactStateRepository
    ) {
        this.alertRepositoryPort = alertRepositoryPort;
        this.unitRepository = unitRepository;
        this.assignmentRepository = assignmentRepository;
        this.personRepository = personRepository;
        this.contactStateRepository = contactStateRepository;
    }

    @Transactional(readOnly = true)
    public List<AlertStakeholderDto> execute(String alertIdStr) {
        Alert alert = null;
        try {
            alert = alertRepositoryPort.findById(AlertId.of(alertIdStr)).orElse(null);
        } catch (Exception ignored) {
        }
        String solution = alert != null ? alert.getApplicationName() : null;

        OrganizationalUnitEntity solutionUnit = resolveSolutionUnit(solution);
        List<AlertStakeholderDto> stakeholders = new ArrayList<>();
        Set<UUID> seenPersons = new HashSet<>();

        if (solutionUnit != null) {
            // 1. Solution TAM & Admin Technique
            List<UnitAdminAssignmentEntity> solAssignments = assignmentRepository.findByUnitIdAndActiveTrue(solutionUnit.getId());
            for (UnitAdminAssignmentEntity a : solAssignments) {
                if ("TAM".equalsIgnoreCase(a.getRole()) || "TAM_SOLUTION".equalsIgnoreCase(a.getRole())) {
                    addStakeholder(stakeholders, seenPersons, a, solutionUnit, 1, "TAM Solution");
                } else if ("ADMIN_TECHNIQUE".equalsIgnoreCase(a.getRole()) || "ADMIN".equalsIgnoreCase(a.getRole())) {
                    addStakeholder(stakeholders, seenPersons, a, solutionUnit, 2, "Administrateur technique");
                }
            }

            // 2. Parent Domain
            if (solutionUnit.getParentUnitId() != null) {
                OrganizationalUnitEntity domainUnit = unitRepository.findById(solutionUnit.getParentUnitId()).orElse(null);
                if (domainUnit != null) {
                    List<UnitAdminAssignmentEntity> domainAssignments = assignmentRepository.findByUnitIdAndActiveTrue(domainUnit.getId());
                    for (UnitAdminAssignmentEntity a : domainAssignments) {
                        addStakeholder(stakeholders, seenPersons, a, domainUnit, 3, "Responsable domaine");
                    }

                    // 3. Parent Pole
                    if (domainUnit.getParentUnitId() != null) {
                        OrganizationalUnitEntity poleUnit = unitRepository.findById(domainUnit.getParentUnitId()).orElse(null);
                        if (poleUnit != null) {
                            List<UnitAdminAssignmentEntity> poleAssignments = assignmentRepository.findByUnitIdAndActiveTrue(poleUnit.getId());
                            for (UnitAdminAssignmentEntity a : poleAssignments) {
                                addStakeholder(stakeholders, seenPersons, a, poleUnit, 4, "Responsable pôle");
                            }

                            // 4. Parent Entity
                            if (poleUnit.getParentUnitId() != null) {
                                OrganizationalUnitEntity entityUnit = unitRepository.findById(poleUnit.getParentUnitId()).orElse(null);
                                if (entityUnit != null) {
                                    List<UnitAdminAssignmentEntity> entityAssignments = assignmentRepository.findByUnitIdAndActiveTrue(entityUnit.getId());
                                    for (UnitAdminAssignmentEntity a : entityAssignments) {
                                        addStakeholder(stakeholders, seenPersons, a, entityUnit, 5, "Responsable d'entité");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fallback: If no stakeholders found, load all active recipients
        if (stakeholders.isEmpty()) {
            List<PersonEntity> allPersons = personRepository.findByActiveTrue();
            for (PersonEntity p : allPersons) {
                if (seenPersons.add(p.getId())) {
                    Optional<PersonContactStateEntity> state = contactStateRepository.findById(p.getId());
                    stakeholders.add(new AlertStakeholderDto(
                            p.getId().toString(),
                            p.getFullName(),
                            p.getEmail(),
                            p.getPhone(),
                            extractExtension(p.getPhone()),
                            "ADMIN_TECHNIQUE",
                            "Administrateur disponible",
                            "GENERAL",
                            solution != null ? solution : "Référentiel",
                            2,
                            state.map(PersonContactStateEntity::getSipReachability).orElse("UNKNOWN"),
                            state.map(PersonContactStateEntity::getLastContactAt).orElse(null)
                    ));
                }
            }
        }

        stakeholders.sort(Comparator.comparingInt(AlertStakeholderDto::hierarchyLevel));
        return stakeholders;
    }

    private void addStakeholder(
            List<AlertStakeholderDto> list,
            Set<UUID> seen,
            UnitAdminAssignmentEntity a,
            OrganizationalUnitEntity unit,
            int level,
            String roleLabel
    ) {
        if (!seen.add(a.getPersonId())) {
            return;
        }
        PersonEntity person = personRepository.findById(a.getPersonId()).orElse(null);
        if (person == null || !person.isActive()) {
            return;
        }
        Optional<PersonContactStateEntity> state = contactStateRepository.findById(person.getId());
        list.add(new AlertStakeholderDto(
                person.getId().toString(),
                person.getFullName(),
                person.getEmail(),
                person.getPhone(),
                extractExtension(person.getPhone()),
                a.getRole(),
                roleLabel,
                unit.getUnitType(),
                unit.getName(),
                level,
                state.map(PersonContactStateEntity::getSipReachability).orElse("UNKNOWN"),
                state.map(PersonContactStateEntity::getLastContactAt).orElse(null)
        ));
    }

    private OrganizationalUnitEntity resolveSolutionUnit(String solution) {
        if (solution == null || solution.isBlank()) {
            return null;
        }
        List<OrganizationalUnitEntity> solutions = unitRepository.findByUnitTypeAndActiveTrue("SOLUTION");
        for (OrganizationalUnitEntity s : solutions) {
            if (s.getName().equalsIgnoreCase(solution.trim()) || solution.trim().equalsIgnoreCase(s.getCode())) {
                return s;
            }
            if (s.getName().toLowerCase().contains(solution.trim().toLowerCase()) || solution.toLowerCase().contains(s.getName().toLowerCase())) {
                return s;
            }
        }
        return null;
    }

    private static String extractExtension(String phone) {
        if (phone == null || phone.isBlank()) {
            return "1001";
        }
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.length() == 4) {
            return cleaned;
        }
        if (cleaned.length() > 4) {
            return cleaned.substring(cleaned.length() - 4);
        }
        return phone;
    }
}
