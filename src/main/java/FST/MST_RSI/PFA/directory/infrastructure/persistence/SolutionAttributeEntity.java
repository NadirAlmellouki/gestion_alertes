package FST.MST_RSI.PFA.directory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "solution_attribute")
public class SolutionAttributeEntity {

    @Id
    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "solution_type", length = 100)
    private String solutionType;

    @Column(length = 100)
    private String psi;

    @Column(name = "service_type", length = 100)
    private String serviceType;

    @Column(length = 100)
    private String tenant;

    @Column(name = "target_scope", length = 150)
    private String targetScope;

    @Column(name = "functional_description", columnDefinition = "TEXT")
    private String functionalDescription;

    @Column(nullable = false)
    private boolean active = true;

    protected SolutionAttributeEntity() {
    }

    public static SolutionAttributeEntity create(
            UUID unitId,
            String solutionType,
            String psi,
            String serviceType,
            String tenant,
            String targetScope,
            String functionalDescription,
            boolean active
    ) {
        SolutionAttributeEntity entity = new SolutionAttributeEntity();
        entity.unitId = unitId;
        entity.solutionType = solutionType;
        entity.psi = psi;
        entity.serviceType = serviceType;
        entity.tenant = tenant;
        entity.targetScope = targetScope;
        entity.functionalDescription = functionalDescription;
        entity.active = active;
        return entity;
    }

    public UUID getUnitId() {
        return unitId;
    }

    public String getSolutionType() {
        return solutionType;
    }

    public void setSolutionType(String solutionType) {
        this.solutionType = solutionType;
    }

    public String getPsi() {
        return psi;
    }

    public void setPsi(String psi) {
        this.psi = psi;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getTargetScope() {
        return targetScope;
    }

    public void setTargetScope(String targetScope) {
        this.targetScope = targetScope;
    }

    public String getFunctionalDescription() {
        return functionalDescription;
    }

    public void setFunctionalDescription(String functionalDescription) {
        this.functionalDescription = functionalDescription;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
