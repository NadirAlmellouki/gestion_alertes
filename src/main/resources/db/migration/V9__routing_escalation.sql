-- Async escalation support: schedule next step and trace step history.

ALTER TABLE routing_execution
    ADD COLUMN IF NOT EXISTS next_escalation_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS candidate_index INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS ix_routing_execution_escalation_due
    ON routing_execution (routing_status, next_escalation_at)
    WHERE next_escalation_at IS NOT NULL;

CREATE TABLE IF NOT EXISTS routing_history (
    id                    UUID PRIMARY KEY,
    routing_execution_id  UUID NOT NULL REFERENCES routing_execution (id) ON DELETE CASCADE,
    routing_step_id       UUID,
    target_person_id      UUID REFERENCES person (id) ON DELETE SET NULL,
    target_unit_id        UUID REFERENCES organizational_unit (id) ON DELETE SET NULL,
    action                VARCHAR(30) NOT NULL,
    action_time           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    details               TEXT
);

CREATE INDEX IF NOT EXISTS ix_routing_history_execution_id ON routing_history (routing_execution_id);
