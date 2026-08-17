package FST.MST_RSI.PFA.monitoring.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resolution_check")
public class ResolutionCheckEntity {

    @Id
    private UUID id;

    @Column(name = "alert_id", nullable = false)
    private UUID alertId;

    @Column(name = "external_problem_id", nullable = false)
    private String externalProblemId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_check_at")
    private Instant nextCheckAt;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "last_dynatrace_state", length = 30)
    private String lastDynatraceState;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    public ResolutionCheckEntity() {
    }

    public static ResolutionCheckEntity create(
            UUID id,
            UUID alertId,
            String externalProblemId,
            String status,
            Instant nextCheckAt,
            Instant startedAt
    ) {
        ResolutionCheckEntity entity = new ResolutionCheckEntity();
        entity.id = id;
        entity.alertId = alertId;
        entity.externalProblemId = externalProblemId;
        entity.status = status;
        entity.attemptCount = 0;
        entity.nextCheckAt = nextCheckAt;
        entity.startedAt = startedAt;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAlertId() {
        return alertId;
    }

    public String getExternalProblemId() {
        return externalProblemId;
    }

    public String getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getNextCheckAt() {
        return nextCheckAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getLastDynatraceState() {
        return lastDynatraceState;
    }

    public String getLastError() {
        return lastError;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public void setNextCheckAt(Instant nextCheckAt) {
        this.nextCheckAt = nextCheckAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public void setLastDynatraceState(String lastDynatraceState) {
        this.lastDynatraceState = lastDynatraceState;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
