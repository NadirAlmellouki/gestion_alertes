package FST.MST_RSI.PFA.directory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "unit_admin_assignment")
public class UnitAdminAssignmentEntity {

    @Id
    private UUID id;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(nullable = false, length = 40)
    private String role;

    @Column(name = "primary_contact", nullable = false)
    private boolean primaryContact;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UnitAdminAssignmentEntity() {
    }

    public static UnitAdminAssignmentEntity create(
            UUID id,
            UUID unitId,
            UUID personId,
            String role,
            boolean primaryContact,
            Instant createdAt
    ) {
        UnitAdminAssignmentEntity entity = new UnitAdminAssignmentEntity();
        entity.id = id;
        entity.unitId = unitId;
        entity.personId = personId;
        entity.role = role;
        entity.primaryContact = primaryContact;
        entity.createdAt = createdAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUnitId() {
        return unitId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public String getRole() {
        return role;
    }

    public boolean isPrimaryContact() {
        return primaryContact;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
