-- Phase 3C: Loan & Advance Management

CREATE TABLE IF NOT EXISTS hrms_staff_loans (
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID          NOT NULL UNIQUE,
    school_id             BIGINT,
    staff_id              BIGINT        NOT NULL REFERENCES staff (id),
    loan_type             VARCHAR(100)  NOT NULL,
    principal_amount      NUMERIC(14,2) NOT NULL,
    approved_amount       NUMERIC(14,2),
    disbursed_at          DATE,
    emi_amount            NUMERIC(14,2),
    emi_count             INTEGER,
    remaining_emis        INTEGER,
    interest_rate         NUMERIC(6,2)  NOT NULL DEFAULT 0,
    status                VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    approval_request_ref  UUID,
    remarks               VARCHAR(2000),
    is_active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP     NOT NULL,
    updated_at            TIMESTAMP     NOT NULL,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_staff_loans_staff_status
    ON hrms_staff_loans (staff_id, status);

CREATE TABLE IF NOT EXISTS hrms_loan_repayments (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID          NOT NULL UNIQUE,
    loan_id         BIGINT        NOT NULL REFERENCES hrms_staff_loans (id),
    due_date        DATE,
    amount          NUMERIC(14,2) NOT NULL,
    paid_date       DATE,
    payroll_run_ref UUID,
    status          VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED',
    remarks         VARCHAR(1000),
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_loan_repayments_loan
    ON hrms_loan_repayments (loan_id, status);

