-- Phase 4: Statutory Compliance Configuration

CREATE TABLE IF NOT EXISTS hrms_statutory_configs (
    id                   BIGSERIAL PRIMARY KEY,
    uuid                 UUID         NOT NULL UNIQUE,
    school_id            BIGINT,
    financial_year       VARCHAR(10)  NOT NULL,
    pf_applicable        BOOLEAN      NOT NULL DEFAULT FALSE,
    pf_employee_rate     NUMERIC(6,4) NOT NULL DEFAULT 0,
    pf_employer_rate     NUMERIC(6,4) NOT NULL DEFAULT 0,
    pf_ceiling_amount    NUMERIC(14,2),
    esi_applicable       BOOLEAN      NOT NULL DEFAULT FALSE,
    esi_employee_rate    NUMERIC(6,4) NOT NULL DEFAULT 0,
    esi_employer_rate    NUMERIC(6,4) NOT NULL DEFAULT 0,
    esi_wage_limit       NUMERIC(14,2),
    pt_applicable        BOOLEAN      NOT NULL DEFAULT FALSE,
    pt_state             VARCHAR(40),
    pt_slabs             JSONB,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP    NOT NULL,
    created_by           VARCHAR(255),
    updated_by           VARCHAR(255),
    UNIQUE (financial_year)
);

CREATE INDEX IF NOT EXISTS idx_hrms_statutory_configs_year
    ON hrms_statutory_configs (financial_year, is_active);

