package FST.MST_RSI.PFA.directory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizational_unit")
public class OrganizationalUnitEntity {

    @Id
    private UUID id;

    @Column(length = 100, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "unit_type", nullable = false, length = 20)
    private String unitType;

    @Column(name = "parent_unit_id")
    private UUID parentUnitId;

    @Column(name = "is_subsidiary", nullable = false)
    private boolean subsidiary;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationalUnitEntity() {
    }

    public static OrganizationalUnitEntity create(
            UUID id,
            String code,
            String name,
            String unitType,
            UUID parentUnitId,
            boolean subsidiary,
            boolean active,
            Instant now
    ) {
        OrganizationalUnitEntity entity = new OrganizationalUnitEntity();
        entity.id = id;
        entity.code = code;
        entity.name = name;
        entity.unitType = unitType;
        entity.parentUnitId = parentUnitId;
        entity.subsidiary = subsidiary;
        entity.active = active;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public UUID getId() {
        return id;
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

    public String getUnitType() {
        return unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public UUID getParentUnitId() {
        return parentUnitId;
    }

    public void setParentUnitId(UUID parentUnitId) {
        this.parentUnitId = parentUnitId;
    }

    public boolean isSubsidiary() {
        return subsidiary;
    }

    public void setSubsidiary(boolean subsidiary) {
        this.subsidiary = subsidiary;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
