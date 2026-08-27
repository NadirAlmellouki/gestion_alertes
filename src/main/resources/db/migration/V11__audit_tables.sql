-- Audit trail and system events.
-- Tables may already exist from the V0 schema (audit_log, audit_log_detail, system_event).
-- This migration creates them if missing, then aligns columns used by the audit module.

CREATE TABLE IF NOT EXISTS audit_log (
    id                     UUID PRIMARY KEY,
    actor_person_id        UUID REFERENCES person (id) ON DELETE SET NULL,
    alert_id               UUID REFERENCES alert (id) ON DELETE SET NULL,
    llm_analysis_id        UUID REFERENCES alert_llm_analysis (id) ON DELETE SET NULL,
    routing_execution_id   UUID REFERENCES routing_execution (id) ON DELETE SET NULL,
    notification_id        UUID REFERENCES notification (id) ON DELETE SET NULL,
    action                 VARCHAR(100) NOT NULL,
    entity_name            VARCHAR(100),
    entity_id              UUID,
    description            TEXT,
    correlation_id         VARCHAR(100),
    ip_address             VARCHAR(100),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(100);

ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS llm_analysis_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'audit_log_llm_analysis_id_fkey'
    ) THEN
        ALTER TABLE audit_log
            ADD CONSTRAINT audit_log_llm_analysis_id_fkey
            FOREIGN KEY (llm_analysis_id) REFERENCES alert_llm_analysis (id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_audit_log_alert_id ON audit_log (alert_id);
CREATE INDEX IF NOT EXISTS ix_audit_log_action ON audit_log (action);
CREATE INDEX IF NOT EXISTS ix_audit_log_created_at ON audit_log (created_at DESC);
CREATE INDEX IF NOT EXISTS ix_audit_log_correlation_id ON audit_log (correlation_id);

CREATE TABLE IF NOT EXISTS audit_log_detail (
    id            UUID PRIMARY KEY,
    audit_log_id  UUID NOT NULL REFERENCES audit_log (id) ON DELETE CASCADE,
    field_name    VARCHAR(100) NOT NULL,
    old_value     TEXT,
    new_value     TEXT
);

CREATE INDEX IF NOT EXISTS ix_audit_log_detail_log_id ON audit_log_detail (audit_log_id);

CREATE TABLE IF NOT EXISTS system_event (
    id               UUID PRIMARY KEY,
    alert_id         UUID REFERENCES alert (id) ON DELETE SET NULL,
    llm_analysis_id  UUID REFERENCES alert_llm_analysis (id) ON DELETE SET NULL,
    source_module    VARCHAR(100) NOT NULL,
    severity         VARCHAR(20) NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    message          TEXT,
    correlation_id   VARCHAR(100),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE system_event
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(100);

ALTER TABLE system_event
    ADD COLUMN IF NOT EXISTS llm_analysis_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'system_event_llm_analysis_id_fkey'
    ) THEN
        ALTER TABLE system_event
            ADD CONSTRAINT system_event_llm_analysis_id_fkey
            FOREIGN KEY (llm_analysis_id) REFERENCES alert_llm_analysis (id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_system_event_severity'
    ) THEN
        ALTER TABLE system_event
            ADD CONSTRAINT chk_system_event_severity
            CHECK (severity IS NULL OR severity IN ('DEBUG', 'INFO', 'WARN', 'ERROR'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_system_event_alert_id ON system_event (alert_id);
CREATE INDEX IF NOT EXISTS ix_system_event_created_at ON system_event (created_at DESC);
CREATE INDEX IF NOT EXISTS ix_system_event_severity ON system_event (severity);
