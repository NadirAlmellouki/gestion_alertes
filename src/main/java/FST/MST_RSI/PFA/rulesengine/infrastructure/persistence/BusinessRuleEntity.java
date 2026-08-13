package FST.MST_RSI.PFA.rulesengine.infrastructure.persistence;

import FST.MST_RSI.PFA.rulesengine.domain.model.RuleOrigin;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "business_rule")
public class BusinessRuleEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rule_evaluation_priority", nullable = false)
    private int evaluationPriority;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "stop_on_match", nullable = false)
    private boolean stopOnMatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_origin", nullable = false, length = 20)
    private RuleOrigin ruleOrigin = RuleOrigin.CONFIGURED;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("executionOrder ASC")
    private List<RuleConditionGroupEntity> conditionGroups = new ArrayList<>();

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("executionOrder ASC")
    private List<RuleActionEntity> actions = new ArrayList<>();

    protected BusinessRuleEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getEvaluationPriority() {
        return evaluationPriority;
    }

    public void setEvaluationPriority(int evaluationPriority) {
        this.evaluationPriority = evaluationPriority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isStopOnMatch() {
        return stopOnMatch;
    }

    public void setStopOnMatch(boolean stopOnMatch) {
        this.stopOnMatch = stopOnMatch;
    }

    public RuleOrigin getRuleOrigin() {
        return ruleOrigin;
    }

    public void setRuleOrigin(RuleOrigin ruleOrigin) {
        this.ruleOrigin = ruleOrigin;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<RuleConditionGroupEntity> getConditionGroups() {
        return conditionGroups;
    }

    public void setConditionGroups(List<RuleConditionGroupEntity> conditionGroups) {
        this.conditionGroups = conditionGroups;
    }

    public List<RuleActionEntity> getActions() {
        return actions;
    }

    public void setActions(List<RuleActionEntity> actions) {
        this.actions = actions;
    }
}
