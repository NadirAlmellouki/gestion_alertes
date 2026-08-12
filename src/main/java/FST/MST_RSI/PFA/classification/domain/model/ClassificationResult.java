package FST.MST_RSI.PFA.classification.domain.model;

import FST.MST_RSI.PFA.common.domain.vo.Confidence;

import java.util.List;

/**
 * Structured LLM classification result.
 * {@code resolvedPsi} is loaded from PostgreSQL after validation, never from the LLM.
 */
public record ClassificationResult(
        ClassificationCategory category,
        String problemType,
        Confidence confidence,
        String matchedSolution,
        String matchedDomaine,
        String matchedPole,
        String matchedEntity,
        String summary,
        String probableCause,
        String justification,
        List<String> uncertainFields,
        boolean requiresHumanValidation,
        ClassificationStatus status,
        String resolvedPsi,
        String errorMessage
) {
    public ClassificationResult {
        uncertainFields = uncertainFields == null ? List.of() : List.copyOf(uncertainFields);
        if (status == null) {
            status = ClassificationStatus.SUCCESS;
        }
    }

    public static ClassificationResult fallback(String reason) {
        return new ClassificationResult(
                ClassificationCategory.UNKNOWN,
                "UNCLASSIFIED",
                new Confidence(0.0),
                null,
                null,
                null,
                null,
                "Classification indisponible",
                null,
                reason,
                List.of("category", "matchedSolution"),
                true,
                ClassificationStatus.FALLBACK,
                null,
                reason
        );
    }

    public ClassificationResult withResolvedPsi(String psi) {
        return new ClassificationResult(
                category,
                problemType,
                confidence,
                matchedSolution,
                matchedDomaine,
                matchedPole,
                matchedEntity,
                summary,
                probableCause,
                justification,
                uncertainFields,
                requiresHumanValidation,
                status,
                psi,
                errorMessage
        );
    }
}
