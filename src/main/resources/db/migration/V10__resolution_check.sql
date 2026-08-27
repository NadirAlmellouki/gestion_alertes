-- Dynatrace problem resolution monitoring (async polling, separate from notification ack).

CREATE TABLE IF NOT EXISTS resolution_check (
    id                    UUID PRIMARY KEY,
    alert_id              UUID NOT NULL REFERENCES alert (id) ON DELETE CASCADE,
    external_problem_id   VARCHAR(255) NOT NULL,
    status                VARCHAR(30) NOT NULL,
    attempt_count         INTEGER NOT NULL DEFAULT 0,
    next_check_at         TIMESTAMPTZ,
    started_at            TIMESTAMPTZ NOT NULL,
    finished_at           TIMESTAMPTZ,
    last_dynatrace_state  VARCHAR(30),
    last_error            TEXT
);

CREATE INDEX IF NOT EXISTS ix_resolution_check_due
    ON resolution_check (status, next_check_at)
    WHERE next_check_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_resolution_check_alert_id ON resolution_check (alert_id);
