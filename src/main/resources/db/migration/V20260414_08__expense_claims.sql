-- Phase 3D: Expense Claims

CREATE TABLE IF NOT EXISTS hrms_expense_claims (
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID          NOT NULL UNIQUE,
    school_id             BIGINT,
    staff_id              BIGINT        NOT NULL REFERENCES staff (id),
    title                 VARCHAR(300)  NOT NULL,
    description           VARCHAR(2000),
    total_amount          NUMERIC(14,2) NOT NULL DEFAULT 0,
    status                VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    submitted_at          TIMESTAMP,
    approval_request_ref  UUID,
    is_active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP     NOT NULL,
    updated_at            TIMESTAMP     NOT NULL,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_expense_claims_staff_status
    ON hrms_expense_claims (staff_id, status);

CREATE TABLE IF NOT EXISTS hrms_expense_claim_items (
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID          NOT NULL UNIQUE,
    claim_id      BIGINT        NOT NULL REFERENCES hrms_expense_claims (id),
    category      VARCHAR(40)   NOT NULL,
    description   VARCHAR(500),
    amount        NUMERIC(14,2) NOT NULL,
    receipt_url   VARCHAR(2000),
    expense_date  DATE          NOT NULL,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_expense_claim_items_claim
    ON hrms_expense_claim_items (claim_id, is_active);

