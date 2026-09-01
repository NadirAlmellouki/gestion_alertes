package FST.MST_RSI.PFA.directory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "person_contact_state")
public class PersonContactStateEntity {

    @Id
    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "sip_reachability", nullable = false, length = 30)
    private String sipReachability = "UNKNOWN";

    @Column(name = "last_contact_at")
    private Instant lastContactAt;

    @Column(name = "last_voip_at")
    private Instant lastVoipAt;

    @Column(name = "last_voip_outcome", length = 30)
    private String lastVoipOutcome;

    @Column(name = "last_voip_hangup_cause")
    private Integer lastVoipHangupCause;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "last_failure_at")
    private Instant lastFailureAt;

    @Column(name = "voip_answered_count", nullable = false)
    private int voipAnsweredCount;

    @Column(name = "voip_failed_count", nullable = false)
    private int voipFailedCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PersonContactStateEntity() {
    }

    public static PersonContactStateEntity create(UUID personId) {
        PersonContactStateEntity entity = new PersonContactStateEntity();
        entity.personId = personId;
        entity.sipReachability = "UNKNOWN";
        entity.updatedAt = Instant.now();
        return entity;
    }

    public UUID getPersonId() {
        return personId;
    }

    public String getSipReachability() {
        return sipReachability;
    }

    public void setSipReachability(String sipReachability) {
        this.sipReachability = sipReachability;
    }

    public Instant getLastContactAt() {
        return lastContactAt;
    }

    public void setLastContactAt(Instant lastContactAt) {
        this.lastContactAt = lastContactAt;
    }

    public Instant getLastVoipAt() {
        return lastVoipAt;
    }

    public void setLastVoipAt(Instant lastVoipAt) {
        this.lastVoipAt = lastVoipAt;
    }

    public String getLastVoipOutcome() {
        return lastVoipOutcome;
    }

    public void setLastVoipOutcome(String lastVoipOutcome) {
        this.lastVoipOutcome = lastVoipOutcome;
    }

    public Integer getLastVoipHangupCause() {
        return lastVoipHangupCause;
    }

    public void setLastVoipHangupCause(Integer lastVoipHangupCause) {
        this.lastVoipHangupCause = lastVoipHangupCause;
    }

    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    public void setLastSuccessAt(Instant lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public Instant getLastFailureAt() {
        return lastFailureAt;
    }

    public void setLastFailureAt(Instant lastFailureAt) {
        this.lastFailureAt = lastFailureAt;
    }

    public int getVoipAnsweredCount() {
        return voipAnsweredCount;
    }

    public void setVoipAnsweredCount(int voipAnsweredCount) {
        this.voipAnsweredCount = voipAnsweredCount;
    }

    public int getVoipFailedCount() {
        return voipFailedCount;
    }

    public void setVoipFailedCount(int voipFailedCount) {
        this.voipFailedCount = voipFailedCount;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
