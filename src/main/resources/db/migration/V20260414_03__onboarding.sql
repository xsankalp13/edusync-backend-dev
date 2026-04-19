-- Phase 2C: Onboarding

CREATE TABLE IF NOT EXISTS hrms_onboarding_templates (
    id            BIGSERIAL PRIMARY KEY,
    uuid          UUID         NOT NULL UNIQUE,
    school_id     BIGINT,
    template_name VARCHAR(200) NOT NULL,
    description   VARCHAR(1000),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS hrms_onboarding_template_tasks (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID         NOT NULL UNIQUE,
    template_id     BIGINT       NOT NULL REFERENCES hrms_onboarding_templates (id),
    task_order      INTEGER      NOT NULL DEFAULT 0,
    task_title      VARCHAR(300) NOT NULL,
    description     VARCHAR(1000),
    due_after_days  INTEGER      NOT NULL DEFAULT 0,
    assigned_party  VARCHAR(20)  NOT NULL DEFAULT 'HR',
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_onboarding_template_tasks
    ON hrms_onboarding_template_tasks (template_id, task_order);

CREATE TABLE IF NOT EXISTS hrms_onboarding_records (
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID         NOT NULL UNIQUE,
    school_id               BIGINT,
    staff_id                BIGINT       NOT NULL REFERENCES staff (id),
    template_id             BIGINT       NOT NULL REFERENCES hrms_onboarding_templates (id),
    start_date              DATE         NOT NULL,
    target_completion_date  DATE,
    status                  VARCHAR(30)  NOT NULL DEFAULT 'IN_PROGRESS',
    completed_at            TIMESTAMP,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_onboarding_records_staff
    ON hrms_onboarding_records (staff_id, status);

CREATE TABLE IF NOT EXISTS hrms_onboarding_task_records (
    id                BIGSERIAL PRIMARY KEY,
    uuid              UUID         NOT NULL UNIQUE,
    record_id         BIGINT       NOT NULL REFERENCES hrms_onboarding_records (id),
    template_task_id  BIGINT       NOT NULL REFERENCES hrms_onboarding_template_tasks (id),
    due_date          DATE,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    completed_at      TIMESTAMP,
    completed_by_ref  UUID,
    completed_by_name VARCHAR(200),
    remarks           VARCHAR(1000),
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_onboarding_task_records_record
    ON hrms_onboarding_task_records (record_id, status);

