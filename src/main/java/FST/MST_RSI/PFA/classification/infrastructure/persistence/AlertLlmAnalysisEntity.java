package FST.MST_RSI.PFA.classification.infrastructure.persistence;

import FST.MST_RSI.PFA.classification.domain.model.ClassificationCategory;
import FST.MST_RSI.PFA.classification.domain.model.ClassificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alert_llm_analysis")
public class AlertLlmAnalysisEntity {

    @Id
    private UUID id;

    @Column(name = "alert_id", nullable = false)
    private UUID alertId;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClassificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ClassificationCategory category;

    @Column(name = "problem_type", length = 100)
    private String problemType;

    @Column(precision = 7, scale = 6)
    private BigDecimal confidence;

    @Column(name = "matched_solution", length = 255)
    private String matchedSolution;

    @Column(name = "matched_domain", length = 255)
    private String matchedDomain;

    @Column(name = "matched_pole", length = 255)
    private String matchedPole;

    @Column(name = "matched_entity", length = 255)
    private String matchedEntity;

    @Column(name = "resolved_psi", length = 50)
    private String resolvedPsi;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "probable_cause", columnDefinition = "TEXT")
    private String probableCause;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "uncertain_fields", columnDefinition = "jsonb")
    private String uncertainFields;

    @Column(name = "requires_human_validation", nullable = false)
    private boolean requiresHumanValidation;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AlertLlmAnalysisEntity() {
    }

    public static AlertLlmAnalysisEntity create(
            UUID id,
            UUID alertId,
            String provider,
            String promptVersion,
            Integer durationMs,
            ClassificationStatus status,
            ClassificationCategory category,
            String problemType,
            BigDecimal confidence,
            String matchedSolution,
            String matchedDomain,
            String matchedPole,
            String matchedEntity,
            String resolvedPsi,
            String summary,
            String probableCause,
            String justification,
            String uncertainFields,
            boolean requiresHumanValidation,
            String errorMessage,
            Instant createdAt
    ) {
        AlertLlmAnalysisEntity entity = new AlertLlmAnalysisEntity();
        entity.id = id;
        entity.alertId = alertId;
        entity.provider = provider;
        entity.promptVersion = promptVersion;
        entity.durationMs = durationMs;
        entity.status = status;
        entity.category = category;
        entity.problemType = problemType;
        entity.confidence = confidence;
        entity.matchedSolution = matchedSolution;
        entity.matchedDomain = matchedDomain;
        entity.matchedPole = matchedPole;
        entity.matchedEntity = matchedEntity;
        entity.resolvedPsi = resolvedPsi;
        entity.summary = summary;
        entity.probableCause = probableCause;
        entity.justification = justification;
        entity.uncertainFields = uncertainFields;
        entity.requiresHumanValidation = requiresHumanValidation;
        entity.errorMessage = errorMessage;
        entity.createdAt = createdAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public ClassificationStatus getStatus() {
        return status;
    }

    public ClassificationCategory getCategory() {
        return category;
    }

    public String getProblemType() {
        return problemType;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public String getMatchedSolution() {
        return matchedSolution;
    }

    public String getMatchedDomain() {
        return matchedDomain;
    }

    public String getMatchedPole() {
        return matchedPole;
    }

    public String getMatchedEntity() {
        return matchedEntity;
    }

    public String getResolvedPsi() {
        return resolvedPsi;
    }

    public boolean isRequiresHumanValidation() {
        return requiresHumanValidation;
    }
}
