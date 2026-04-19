-- Phase 2D: Exit / Offboarding

CREATE TABLE IF NOT EXISTS hrms_exit_requests (
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID         NOT NULL UNIQUE,
    school_id             BIGINT,
    staff_id              BIGINT       NOT NULL REFERENCES staff (id),
    resignation_date      DATE         NOT NULL,
    last_working_date     DATE,
    exit_reason           VARCHAR(2000),
    status                VARCHAR(30)  NOT NULL DEFAULT 'SUBMITTED',
    initiated_by_ref      UUID,
    approval_request_ref  UUID,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP    NOT NULL,
    updated_at            TIMESTAMP    NOT NULL,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_exit_requests_staff
    ON hrms_exit_requests (staff_id, status);

CREATE TABLE IF NOT EXISTS hrms_exit_clearance_items (
    id                     BIGSERIAL PRIMARY KEY,
    uuid                   UUID         NOT NULL UNIQUE,
    exit_request_id        BIGINT       NOT NULL REFERENCES hrms_exit_requests (id),
    item_type              VARCHAR(40)  NOT NULL,
    description            VARCHAR(500),
    responsible_party_ref  UUID,
    completed_at           TIMESTAMP,
    completed_by_name      VARCHAR(200),
    remarks                VARCHAR(1000),
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMP    NOT NULL,
    updated_at             TIMESTAMP    NOT NULL,
    created_by             VARCHAR(255),
    updated_by             VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_exit_clearance_items_request
    ON hrms_exit_clearance_items (exit_request_id);

CREATE TABLE IF NOT EXISTS hrms_fnf_settlements (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID          NOT NULL UNIQUE,
    exit_request_id     BIGINT        NOT NULL UNIQUE REFERENCES hrms_exit_requests (id),
    gross_salary_due    NUMERIC(14,2) NOT NULL DEFAULT 0,
    deductions          NUMERIC(14,2) NOT NULL DEFAULT 0,
    leave_encashment    NUMERIC(14,2) NOT NULL DEFAULT 0,
    gratuity            NUMERIC(14,2) NOT NULL DEFAULT 0,
    other_additions     NUMERIC(14,2) NOT NULL DEFAULT 0,
    other_deductions    NUMERIC(14,2) NOT NULL DEFAULT 0,
    net_payable         NUMERIC(14,2) NOT NULL DEFAULT 0,
    status              VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    approved_by_ref     UUID,
    disbursed_at        TIMESTAMP,
    remarks             VARCHAR(2000),
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL,
    updated_at          TIMESTAMP     NOT NULL,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

