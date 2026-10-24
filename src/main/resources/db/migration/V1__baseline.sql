-- Baseline schema for LoanIQ.
--
-- Money is numeric(15,2) rather than a floating type so that totals are exact.
-- Decisions and statuses are stored as text with a check constraint instead of
-- a native enum, so adding a value later does not require a type migration.

CREATE TABLE loan_application (
    id                      UUID PRIMARY KEY,
    idempotency_key         VARCHAR(128) NOT NULL UNIQUE,
    applicant_ref           VARCHAR(64)  NOT NULL,
    applicant_name          VARCHAR(160) NOT NULL,
    amount_requested        NUMERIC(15,2) NOT NULL,
    tenure_months           INTEGER       NOT NULL,
    declared_monthly_income NUMERIC(15,2),
    monthly_obligations     NUMERIC(15,2),
    employment_type         VARCHAR(32),
    bureau_score            INTEGER,
    status                  VARCHAR(32)  NOT NULL,
    decision                VARCHAR(16),
    risk_score              INTEGER,
    approved_amount         NUMERIC(15,2),
    decision_rationale      VARCHAR(4000),
    created_at              TIMESTAMPTZ  NOT NULL,
    decided_at              TIMESTAMPTZ,
    version                 BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT chk_application_status
        CHECK (status IN ('RECEIVED','EXTRACTING','SCORING','DECIDED','FAILED')),
    CONSTRAINT chk_application_decision
        CHECK (decision IS NULL OR decision IN ('APPROVE','REFER','DECLINE')),
    CONSTRAINT chk_amount_positive CHECK (amount_requested > 0),
    CONSTRAINT chk_tenure_positive CHECK (tenure_months > 0)
);

CREATE INDEX idx_loan_application_status    ON loan_application (status);
CREATE INDEX idx_loan_application_applicant ON loan_application (applicant_ref);

CREATE TABLE rule_outcome (
    id             UUID PRIMARY KEY,
    application_id UUID         NOT NULL REFERENCES loan_application (id) ON DELETE CASCADE,
    rule_code      VARCHAR(64)  NOT NULL,
    passed         BOOLEAN      NOT NULL,
    score_delta    INTEGER      NOT NULL,
    hard_stop      BOOLEAN      NOT NULL,
    explanation    VARCHAR(500) NOT NULL
);

CREATE INDEX idx_rule_outcome_application ON rule_outcome (application_id);

-- Append-only: no update or delete path exists in the application code.
CREATE TABLE audit_event (
    id             UUID PRIMARY KEY,
    application_id UUID          NOT NULL,
    event_type     VARCHAR(64)   NOT NULL,
    actor          VARCHAR(64)   NOT NULL,
    detail         VARCHAR(4000) NOT NULL,
    occurred_at    TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_audit_event_application ON audit_event (application_id, occurred_at);
