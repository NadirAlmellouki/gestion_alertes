package FST.MST_RSI.PFA.audit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "audit_log_detail")
public class AuditLogDetailEntity {

    @Id
    private UUID id;

    @Column(name = "audit_log_id", nullable = false)
    private UUID auditLogId;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    public AuditLogDetailEntity() {
    }

    public static AuditLogDetailEntity create(UUID id, UUID auditLogId, String fieldName, String oldValue, String newValue) {
        AuditLogDetailEntity entity = new AuditLogDetailEntity();
        entity.id = id;
        entity.auditLogId = auditLogId;
        entity.fieldName = fieldName;
        entity.oldValue = oldValue;
        entity.newValue = newValue;
        return entity;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }
}
