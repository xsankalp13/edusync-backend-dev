-- Phase 3E: Overtime & Comp-Off

CREATE TABLE IF NOT EXISTS hrms_overtime_records (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID          NOT NULL UNIQUE,
    school_id           BIGINT,
    staff_id            BIGINT        NOT NULL REFERENCES staff (id),
    work_date           DATE          NOT NULL,
    hours_worked        NUMERIC(5,2)  NOT NULL,
    reason              VARCHAR(1000),
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    approved_by_ref     UUID,
    approved_at         TIMESTAMP,
    compensation_type   VARCHAR(20)   NOT NULL DEFAULT 'CASH',
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL,
    updated_at          TIMESTAMP     NOT NULL,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_overtime_records_staff_status
    ON hrms_overtime_records (staff_id, status);

CREATE TABLE IF NOT EXISTS hrms_compoff_records (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID         NOT NULL UNIQUE,
    school_id           BIGINT,
    staff_id            BIGINT       NOT NULL REFERENCES staff (id),
    overtime_record_id  BIGINT       REFERENCES hrms_overtime_records (id),
    leave_type_id       BIGINT       REFERENCES hrms_leave_type_configs (id),
    credit_date         DATE         NOT NULL,
    expiry_date         DATE,
    credited            BOOLEAN      NOT NULL DEFAULT FALSE,
    credited_at         TIMESTAMP,
    remarks             VARCHAR(1000),
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_compoff_records_staff
    ON hrms_compoff_records (staff_id, credited);

