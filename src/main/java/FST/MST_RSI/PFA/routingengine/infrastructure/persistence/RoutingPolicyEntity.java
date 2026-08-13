package FST.MST_RSI.PFA.routingengine.infrastructure.persistence;

import FST.MST_RSI.PFA.routingengine.domain.model.PolicyOrigin;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "routing_policy")
public class RoutingPolicyEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_origin", nullable = false, length = 20)
    private PolicyOrigin policyOrigin = PolicyOrigin.CONFIGURED;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    private List<RoutingStepEntity> steps = new ArrayList<>();

    public RoutingPolicyEntity() {
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public PolicyOrigin getPolicyOrigin() {
        return policyOrigin;
    }

    public void setPolicyOrigin(PolicyOrigin policyOrigin) {
        this.policyOrigin = policyOrigin;
    }

    public List<RoutingStepEntity> getSteps() {
        return steps;
    }

    public void setSteps(List<RoutingStepEntity> steps) {
        this.steps = steps;
    }
}
