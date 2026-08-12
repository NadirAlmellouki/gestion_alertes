-- Align operational alerting columns with schema.sql and migrate legacy Flyway V3 tables if present.

ALTER TABLE alert
    ADD COLUMN IF NOT EXISTS notification_state VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE';

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'alerts'
    ) THEN
        INSERT INTO alert (
            id, problem_id, title, severity, impact_level, status, source,
            received_at, raw_payload, notification_state, start_time
        )
        SELECT
            a.id,
            a.external_problem_id,
            a.title,
            a.severity,
            a.impact,
            a.dynatrace_state,
            'DYNATRACE',
            a.received_at,
            a.raw_payload::jsonb,
            COALESCE(a.notification_state, 'EN_ATTENTE'),
            a.problem_started_at
        FROM alerts a
        ON CONFLICT (problem_id) DO UPDATE SET
            title = EXCLUDED.title,
            severity = EXCLUDED.severity,
            impact_level = EXCLUDED.impact_level,
            status = EXCLUDED.status,
            raw_payload = EXCLUDED.raw_payload,
            notification_state = EXCLUDED.notification_state,
            start_time = EXCLUDED.start_time;

        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'alert_timeline' AND column_name = 'message'
        ) THEN
            DROP TABLE alert_timeline;
        END IF;

        DROP TABLE alerts CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_alert_notification_state ON alert (notification_state);
