package FST.MST_RSI.PFA.routingengine.domain.service;

import FST.MST_RSI.PFA.directory.infrastructure.persistence.OrganizationalUnitEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.OrganizationalUnitRepository;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.PersonRepository;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.UnitAdminAssignmentEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.UnitAdminAssignmentRepository;
import FST.MST_RSI.PFA.routingengine.domain.model.ResolvedPerson;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingContext;
import FST.MST_RSI.PFA.routingengine.domain.model.RoutingStepDefinition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PersonResolver {

    private final OrganizationalUnitRepository unitRepository;
    private final UnitAdminAssignmentRepository assignmentRepository;
    private final PersonRepository personRepository;

    public PersonResolver(
            OrganizationalUnitRepository unitRepository,
            UnitAdminAssignmentRepository assignmentRepository,
            PersonRepository personRepository
    ) {
        this.unitRepository = unitRepository;
        this.assignmentRepository = assignmentRepository;
        this.personRepository = personRepository;
    }

    public List<ResolvedPerson> resolve(RoutingContext context, RoutingStepDefinition step) {
        String role = context.forcedRole() != null && !context.forcedRole().isBlank()
                ? context.forcedRole()
                : step.targetRole();
        UUID unitId = resolveUnitId(context, step.targetUnitType());
        if (unitId == null) {
            return List.of();
        }
        List<ResolvedPerson> persons = new ArrayList<>();
        for (UnitAdminAssignmentEntity assignment : assignmentRepository
                .findByUnitIdAndRoleAndActiveTrueOrderByPrimaryContactDesc(unitId, role)) {
            Optional<PersonEntity> personOpt = personRepository.findById(assignment.getPersonId());
            if (personOpt.isEmpty() || !personOpt.get().isActive()) {
                continue;
            }
            PersonEntity person = personOpt.get();
            persons.add(new ResolvedPerson(
                    person.getId(),
                    person.getFullName(),
                    person.getEmail(),
                    role,
                    unitId,
                    assignment.isPrimaryContact()
            ));
        }
        return persons;
    }

    private UUID resolveUnitId(RoutingContext context, String targetUnitType) {
        return switch (targetUnitType) {
            case "SOLUTION" -> context.solutionUnitId();
            case "DOMAIN" -> context.domainUnitId();
            case "POLE" -> context.poleUnitId();
            case "ENTITY" -> context.entityUnitId();
            default -> null;
        };
    }

    public Optional<OrganizationalUnitEntity> findSolutionByName(String solutionName) {
        if (solutionName == null || solutionName.isBlank()) {
            return Optional.empty();
        }
        return unitRepository.findByUnitTypeAndNameIgnoreCaseAndActiveTrue("SOLUTION", solutionName).stream().findFirst();
    }

    public HierarchyIds resolveHierarchy(UUID solutionUnitId) {
        OrganizationalUnitEntity solution = unitRepository.findById(solutionUnitId).orElse(null);
        if (solution == null) {
            return new HierarchyIds(null, null, null, null);
        }
        UUID domainId = solution.getParentUnitId();
        UUID poleId = unitRepository.findById(domainId).map(OrganizationalUnitEntity::getParentUnitId).orElse(null);
        UUID entityId = poleId == null ? null
                : unitRepository.findById(poleId).map(OrganizationalUnitEntity::getParentUnitId).orElse(null);
        return new HierarchyIds(solutionUnitId, domainId, poleId, entityId);
    }

    public record HierarchyIds(UUID solutionId, UUID domainId, UUID poleId, UUID entityId) {
    }
}
