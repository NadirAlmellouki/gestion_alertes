package FST.MST_RSI.PFA.rulesengine.domain.model;

import FST.MST_RSI.PFA.classification.domain.model.ClassificationCategory;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Snapshot of all data needed to evaluate business rules without per-condition SQL.
 */
public record BusinessRuleContext(
        UUID alertId,
        String problemId,
        String title,
        String severity,
        String impactLevel,
        String status,
        String source,
        Instant receivedAt,
        ClassificationCategory llmCategory,
        String llmProblemType,
        double llmConfidence,
        String llmMatchedSolution,
        String llmMatchedDomain,
        String llmMatchedPole,
        String llmMatchedEntity,
        ClassificationStatus llmStatus,
        boolean llmRequiresHumanValidation,
        String resolvedPsi,
        UUID resolvedSolutionUnitId,
        String resolvedSolutionName,
        String resolvedDomainName,
        String resolvedPoleName,
        String resolvedEntityName,
        String solutionType,
        String serviceType,
        String tenant,
        String targetScope,
        boolean solutionActive,
        boolean entitySubsidiary,
        List<String> entityTags,
        Map<String, Object> computed
) {
    public BusinessRuleContext {
        entityTags = entityTags == null ? List.of() : List.copyOf(entityTags);
        computed = computed == null ? Map.of() : Map.copyOf(computed);
    }

    public Object fieldValue(String fieldName) {
        if (fieldName == null) {
            return null;
        }
        return switch (fieldName) {
            case "alert.severity", "severity" -> severity;
            case "alert.impactLevel", "impactLevel" -> impactLevel;
            case "alert.status", "status" -> status;
            case "alert.title", "title" -> title;
            case "alert.source", "source" -> source;
            case "alert.problemId", "problemId" -> problemId;
            case "llm.category", "category" -> llmCategory == null ? null : llmCategory.name();
            case "llm.problemType", "problemType" -> llmProblemType;
            case "llm.confidence", "confiance_ia" -> llmConfidence;
            case "llm.matchedSolution", "solutionProposee" -> llmMatchedSolution;
            case "llm.matchedDomain", "domaineProposee" -> llmMatchedDomain;
            case "llm.matchedPole" -> llmMatchedPole;
            case "llm.matchedEntity" -> llmMatchedEntity;
            case "llm.status" -> llmStatus == null ? null : llmStatus.name();
            case "llm.requiresHumanValidation", "validationRequise" -> llmRequiresHumanValidation;
            case "context.psi", "psi" -> resolvedPsi;
            case "context.solution", "solution" -> resolvedSolutionName;
            case "context.domain", "domaine" -> resolvedDomainName;
            case "context.pole", "pole" -> resolvedPoleName;
            case "context.entity", "entite" -> resolvedEntityName;
            case "context.solutionType", "typeSolution" -> solutionType;
            case "context.serviceType", "typeService" -> serviceType;
            case "context.tenant" -> tenant;
            case "context.targetScope", "targetScope" -> targetScope;
            case "context.solutionActive", "solutionActive" -> solutionActive;
            case "context.subsidiary", "filiale" -> entitySubsidiary;
            default -> computed.get(fieldName);
        };
    }
}
