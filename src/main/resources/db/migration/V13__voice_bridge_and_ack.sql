-- Bidirectional manual-call teardown + VoIP prise en charge.

ALTER TABLE voice_call_session
    ADD COLUMN IF NOT EXISTS supervisor_channel_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS supervisor_extension VARCHAR(32),
    ADD COLUMN IF NOT EXISTS bridge_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS recording_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS hangup_source VARCHAR(30);

CREATE INDEX IF NOT EXISTS ix_voice_call_session_supervisor_channel
    ON voice_call_session (supervisor_channel_id);

CREATE INDEX IF NOT EXISTS ix_voice_call_session_routing
    ON voice_call_session (routing_execution_id);

ALTER TABLE notification DROP CONSTRAINT IF EXISTS chk_notification_status;
ALTER TABLE notification
    ADD CONSTRAINT chk_notification_status CHECK (notification_status IN (
        'PENDING', 'SENT', 'FAILED', 'DEFERRED', 'SKIPPED', 'ACKNOWLEDGED'
    ));
