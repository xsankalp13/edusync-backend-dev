-- Phase 3A: Performance Appraisals

CREATE TABLE IF NOT EXISTS hrms_appraisal_cycles (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID         NOT NULL UNIQUE,
    school_id   BIGINT,
    cycle_name  VARCHAR(200) NOT NULL,
    academic_year VARCHAR(20),
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    status      VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_appraisal_cycles_status
    ON hrms_appraisal_cycles (status, is_active);

CREATE TABLE IF NOT EXISTS hrms_appraisal_goals (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID         NOT NULL UNIQUE,
    cycle_id        BIGINT       NOT NULL REFERENCES hrms_appraisal_cycles (id),
    staff_id        BIGINT       NOT NULL REFERENCES staff (id),
    goal_title      VARCHAR(300) NOT NULL,
    description     VARCHAR(2000),
    weightage       INTEGER      NOT NULL DEFAULT 0,
    target_metric   VARCHAR(500),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_appraisal_goals_cycle_staff
    ON hrms_appraisal_goals (cycle_id, staff_id);

CREATE TABLE IF NOT EXISTS hrms_self_appraisal_reviews (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID         NOT NULL UNIQUE,
    cycle_id        BIGINT       NOT NULL REFERENCES hrms_appraisal_cycles (id),
    staff_id        BIGINT       NOT NULL REFERENCES staff (id),
    self_rating     INTEGER,
    achievements    VARCHAR(3000),
    challenges      VARCHAR(3000),
    training_needs  VARCHAR(2000),
    submitted_at    TIMESTAMP,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP    NOT NULL,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    UNIQUE (cycle_id, staff_id)
);

CREATE TABLE IF NOT EXISTS hrms_manager_appraisal_reviews (
    id                    BIGSERIAL PRIMARY KEY,
    uuid                  UUID         NOT NULL UNIQUE,
    cycle_id              BIGINT       NOT NULL REFERENCES hrms_appraisal_cycles (id),
    staff_id              BIGINT       NOT NULL REFERENCES staff (id),
    reviewer_staff_id     BIGINT       REFERENCES staff (id),
    manager_rating        INTEGER,
    strengths             VARCHAR(3000),
    areas_of_improvement  VARCHAR(3000),
    overall_remarks       VARCHAR(2000),
    submitted_at          TIMESTAMP,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP    NOT NULL,
    updated_at            TIMESTAMP    NOT NULL,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255),
    UNIQUE (cycle_id, staff_id)
);

