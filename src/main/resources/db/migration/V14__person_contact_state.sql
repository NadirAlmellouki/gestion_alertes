CREATE TABLE IF NOT EXISTS person_contact_state (
    person_id               UUID PRIMARY KEY REFERENCES person (id) ON DELETE CASCADE,
    sip_reachability        VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN',
    last_contact_at         TIMESTAMPTZ,
    last_voip_at            TIMESTAMPTZ,
    last_voip_outcome       VARCHAR(30),
    last_voip_hangup_cause  INTEGER,
    last_success_at         TIMESTAMPTZ,
    last_failure_at         TIMESTAMPTZ,
    voip_answered_count     INTEGER NOT NULL DEFAULT 0,
    voip_failed_count       INTEGER NOT NULL DEFAULT 0,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sip_reachability CHECK (sip_reachability IN (
        'UNKNOWN', 'AVAILABLE', 'BUSY', 'NO_ANSWER', 'UNREGISTERED', 'UNREACHABLE'
    ))
);
