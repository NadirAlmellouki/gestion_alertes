-- Notification persistence for email/SMS/VoIP delivery tracking.

CREATE TABLE IF NOT EXISTS notification_template (
    id      UUID PRIMARY KEY,
    code    VARCHAR(50) NOT NULL UNIQUE,
    channel VARCHAR(20) NOT NULL,
    title   VARCHAR(255),
    body    TEXT,
    active  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS notification (
    id                    UUID PRIMARY KEY,
    alert_id              UUID NOT NULL REFERENCES alert (id) ON DELETE CASCADE,
    routing_execution_id  UUID REFERENCES routing_execution (id) ON DELETE SET NULL,
    template_id           UUID REFERENCES notification_template (id) ON DELETE SET NULL,
    notification_type     VARCHAR(20) NOT NULL,
    notification_status   VARCHAR(30) NOT NULL,
    call_mode             VARCHAR(10) NOT NULL DEFAULT 'AUTO',
    triggered_by_person_id UUID REFERENCES person (id) ON DELETE SET NULL,
    priority              INTEGER NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_notification_type CHECK (notification_type IN ('SMS', 'VOIP', 'EMAIL')),
    CONSTRAINT chk_notification_status CHECK (notification_status IN (
        'PENDING', 'SENT', 'FAILED', 'DEFERRED', 'SKIPPED'
    )),
    CONSTRAINT chk_notification_call_mode CHECK (call_mode IN ('AUTO', 'MANUAL'))
);

CREATE TABLE IF NOT EXISTS notification_recipient (
    id               UUID PRIMARY KEY,
    notification_id  UUID NOT NULL REFERENCES notification (id) ON DELETE CASCADE,
    person_id        UUID REFERENCES person (id) ON DELETE SET NULL,
    channel          VARCHAR(20) NOT NULL,
    destination      VARCHAR(255) NOT NULL,
    recipient_order  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS notification_attempt (
    id                   UUID PRIMARY KEY,
    recipient_id         UUID NOT NULL REFERENCES notification_recipient (id) ON DELETE CASCADE,
    attempt_number       INTEGER NOT NULL,
    provider             VARCHAR(100),
    provider_message_id  VARCHAR(255),
    status               VARCHAR(30) NOT NULL,
    error_message        TEXT,
    started_at           TIMESTAMPTZ NOT NULL,
    finished_at          TIMESTAMPTZ,
    CONSTRAINT chk_notification_attempt_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS ix_notification_alert_id ON notification (alert_id);
CREATE INDEX IF NOT EXISTS ix_notification_status ON notification (notification_status);
CREATE INDEX IF NOT EXISTS ix_notification_recipient_notification_id ON notification_recipient (notification_id);

INSERT INTO notification_template (id, code, channel, title, body, active)
SELECT '11111111-0001-4000-8000-000000000001',
       'DEFAULT-EMAIL-ALERT',
       'EMAIL',
       '[AlertOps] {{severity}} — {{title}}',
       'Bonjour {{recipientName}},\n\nUne alerte nécessite votre attention.\n\nProblème : {{problemId}}\nTitre : {{title}}\nSévérité : {{severity}}\nImpact : {{impact}}\nSolution : {{solution}}\nCatégorie IA : {{category}}\nConfiance IA : {{confidence}}\n\nCet e-mail confirme uniquement l''envoi technique de la notification.\nIl ne constitue pas une prise en charge métier de l''incident.\n\n— AlertOps',
       TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE code = 'DEFAULT-EMAIL-ALERT');
