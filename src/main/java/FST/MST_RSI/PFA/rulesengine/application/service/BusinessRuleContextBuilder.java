package FST.MST_RSI.PFA.rulesengine.application.service;

import FST.MST_RSI.PFA.alerting.domain.model.Alert;
import FST.MST_RSI.PFA.classification.infrastructure.persistence.AlertLlmAnalysisEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.OrganizationalUnitEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.OrganizationalUnitRepository;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.SolutionAttributeEntity;
import FST.MST_RSI.PFA.directory.infrastructure.persistence.SolutionAttributeRepository;
import FST.MST_RSI.PFA.routingengine.domain.service.PersonResolver;
import FST.MST_RSI.PFA.rulesengine.domain.model.BusinessRuleContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BusinessRuleContextBuilder {

    private final PersonResolver personResolver;
    private final SolutionAttributeRepository solutionAttributeRepository;
    private final OrganizationalUnitRepository unitRepository;

    public BusinessRuleContextBuilder(
            PersonResolver personResolver,
            SolutionAttributeRepository solutionAttributeRepository,
            OrganizationalUnitRepository unitRepository
    ) {
        this.personResolver = personResolver;
        this.solutionAttributeRepository = solutionAttributeRepository;
        this.unitRepository = unitRepository;
    }

    public BusinessRuleContext build(Alert alert, AlertLlmAnalysisEntity analysis) {
        Optional<OrganizationalUnitEntity> solutionOpt = personResolver.findSolutionByName(analysis.getMatchedSolution());
        UUID solutionUnitId = solutionOpt.map(OrganizationalUnitEntity::getId).orElse(null);
        String solutionName = solutionOpt.map(OrganizationalUnitEntity::getName).orElse(analysis.getMatchedSolution());

        PersonResolver.HierarchyIds hierarchy = solutionUnitId == null
                ? new PersonResolver.HierarchyIds(null, null, null, null)
                : personResolver.resolveHierarchy(solutionUnitId);

        String domainName = hierarchy.domainId() == null ? analysis.getMatchedDomain()
                : unitRepository.findById(hierarchy.domainId()).map(OrganizationalUnitEntity::getName).orElse(analysis.getMatchedDomain());
        String poleName = hierarchy.poleId() == null ? analysis.getMatchedPole()
                : unitRepository.findById(hierarchy.poleId()).map(OrganizationalUnitEntity::getName).orElse(analysis.getMatchedPole());
        String entityName = hierarchy.entityId() == null ? analysis.getMatchedEntity()
                : unitRepository.findById(hierarchy.entityId()).map(OrganizationalUnitEntity::getName).orElse(analysis.getMatchedEntity());

        SolutionAttributeEntity attributes = solutionUnitId == null
                ? null
                : solutionAttributeRepository.findById(solutionUnitId).orElse(null);

        String psi = analysis.getResolvedPsi();
        if (psi == null && attributes != null) {
            psi = attributes.getPsi();
        }

        boolean subsidiary = hierarchy.entityId() != null
                && unitRepository.findById(hierarchy.entityId()).map(OrganizationalUnitEntity::isSubsidiary).orElse(false);

        return new BusinessRuleContext(
                alert.getId().value(),
                alert.getExternalProblemId(),
                alert.getTitle(),
                alert.getSeverity(),
                alert.getImpact(),
                alert.getDynatraceState(),
                "DYNATRACE",
                alert.getReceivedAt(),
                analysis.getCategory(),
                analysis.getProblemType(),
                analysis.getConfidence() == null ? 0.0 : analysis.getConfidence().doubleValue(),
                analysis.getMatchedSolution(),
                analysis.getMatchedDomain(),
                analysis.getMatchedPole(),
                analysis.getMatchedEntity(),
                analysis.getStatus(),
                analysis.isRequiresHumanValidation(),
                psi,
                solutionUnitId,
                solutionName,
                domainName,
                poleName,
                entityName,
                attributes == null ? null : attributes.getSolutionType(),
                attributes == null ? null : attributes.getServiceType(),
                attributes == null ? null : attributes.getTenant(),
                attributes == null ? null : attributes.getTargetScope(),
                attributes == null || attributes.isActive(),
                subsidiary,
                List.of(),
                java.util.Map.of()
        );
    }
}
