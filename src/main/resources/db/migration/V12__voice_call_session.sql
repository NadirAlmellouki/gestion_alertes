CREATE TABLE IF NOT EXISTS voice_call_session (
    id                      UUID PRIMARY KEY,
    notification_id         UUID REFERENCES notification (id) ON DELETE SET NULL,
    alert_id                UUID REFERENCES alert (id) ON DELETE SET NULL,
    routing_execution_id    UUID REFERENCES routing_execution (id) ON DELETE SET NULL,
    person_id               UUID REFERENCES person (id) ON DELETE SET NULL,
    extension               VARCHAR(32) NOT NULL,
    provider_call_id        VARCHAR(128),
    sound_name              VARCHAR(255),
    outcome                 VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    hangup_cause            INTEGER,
    failure_reason          TEXT,
    started_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ringing_at              TIMESTAMPTZ,
    answered_at             TIMESTAMPTZ,
    ended_at                TIMESTAMPTZ,
    duration_seconds        INTEGER,
    live_conversation       BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_voice_call_outcome CHECK (outcome IN (
        'INITIATED', 'RINGING', 'ANSWERED', 'REJECTED', 'BUSY', 'NO_ANSWER', 'FAILED', 'HANGUP'
    ))
);

CREATE INDEX IF NOT EXISTS ix_voice_call_session_notification ON voice_call_session (notification_id);
CREATE INDEX IF NOT EXISTS ix_voice_call_session_provider ON voice_call_session (provider_call_id);
