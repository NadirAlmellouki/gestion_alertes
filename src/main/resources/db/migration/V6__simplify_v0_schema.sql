-- V0 simplification: remove alert_timeline, alert_attachment, legacy LLM tables;
-- introduce single alert_llm_analysis table for classification results.

DROP TABLE IF EXISTS alert_timeline CASCADE;
DROP TABLE IF EXISTS alert_attachment CASCADE;
DROP TABLE IF EXISTS llm_recommendation CASCADE;
DROP TABLE IF EXISTS llm_classification CASCADE;
DROP TABLE IF EXISTS prompt_context CASCADE;
DROP TABLE IF EXISTS llm_execution CASCADE;

CREATE TABLE IF NOT EXISTS alert_llm_analysis (
    id                        UUID PRIMARY KEY,
    alert_id                  UUID NOT NULL REFERENCES alert (id) ON DELETE CASCADE,
    provider                  VARCHAR(50) NOT NULL,
    prompt_version            VARCHAR(50) NOT NULL,
    duration_ms               INTEGER,
    input_tokens              INTEGER,
    output_tokens             INTEGER,
    status                    VARCHAR(30) NOT NULL,
    category                  VARCHAR(50),
    problem_type              VARCHAR(100),
    confidence                DECIMAL(7, 6),
    matched_solution          VARCHAR(255),
    matched_domain            VARCHAR(255),
    matched_pole              VARCHAR(255),
    matched_entity            VARCHAR(255),
    resolved_psi              VARCHAR(50),
    summary                   TEXT,
    probable_cause            TEXT,
    justification             TEXT,
    uncertain_fields          JSONB,
    requires_human_validation BOOLEAN NOT NULL DEFAULT FALSE,
    error_message             TEXT,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_alert_llm_analysis_alert_id ON alert_llm_analysis (alert_id);
CREATE INDEX IF NOT EXISTS ix_alert_llm_analysis_created_at ON alert_llm_analysis (created_at DESC);
