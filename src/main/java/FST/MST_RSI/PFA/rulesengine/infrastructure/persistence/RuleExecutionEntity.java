package FST.MST_RSI.PFA.rulesengine.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rule_execution")
public class RuleExecutionEntity {

    @Id
    private UUID id;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "alert_id", nullable = false)
    private UUID alertId;

    @Column(name = "classification_id")
    private UUID classificationId;

    @Column(nullable = false)
    private boolean matched;

    @Column(name = "execution_duration_ms")
    private Integer executionDurationMs;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    protected RuleExecutionEntity() {
    }

    public static RuleExecutionEntity create(
            UUID id,
            UUID ruleId,
            UUID alertId,
            UUID classificationId,
            boolean matched,
            int durationMs,
            Instant executedAt
    ) {
        RuleExecutionEntity entity = new RuleExecutionEntity();
        entity.id = id;
        entity.ruleId = ruleId;
        entity.alertId = alertId;
        entity.classificationId = classificationId;
        entity.matched = matched;
        entity.executionDurationMs = durationMs;
        entity.executedAt = executedAt;
        return entity;
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public boolean isMatched() {
        return matched;
    }
}
