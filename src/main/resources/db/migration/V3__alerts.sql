CREATE TABLE alerts (
    id                  UUID PRIMARY KEY,
    external_problem_id VARCHAR(255) NOT NULL UNIQUE,
    title               VARCHAR(500) NOT NULL,
    application_name    VARCHAR(255),
    environment         VARCHAR(100),
    severity            VARCHAR(50),
    impact              VARCHAR(50),
    dynatrace_state     VARCHAR(20),
    notification_state  VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE',
    problem_url         VARCHAR(1000),
    host_name           VARCHAR(255),
    raw_payload         TEXT NOT NULL,
    received_at         TIMESTAMPTZ NOT NULL,
    problem_started_at  TIMESTAMPTZ
);

CREATE TABLE alert_timeline (
    id          BIGSERIAL PRIMARY KEY,
    alert_id    UUID NOT NULL REFERENCES alerts(id) ON DELETE CASCADE,
    event_type  VARCHAR(50) NOT NULL,
    message     TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_alerts_received_at ON alerts(received_at DESC);
CREATE INDEX idx_alerts_notification_state ON alerts(notification_state);
CREATE INDEX idx_alert_timeline_alert_id ON alert_timeline(alert_id);
