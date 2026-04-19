-- Phase 3B: Training & Development

CREATE TABLE IF NOT EXISTS hrms_training_courses (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID         NOT NULL UNIQUE,
    school_id   BIGINT,
    course_code VARCHAR(50)  NOT NULL,
    title       VARCHAR(300) NOT NULL,
    description VARCHAR(3000),
    facilitator VARCHAR(200),
    start_date  DATE,
    end_date    DATE,
    max_seats   INTEGER,
    status      VARCHAR(30)  NOT NULL DEFAULT 'UPCOMING',
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_hrms_training_course_code
    ON hrms_training_courses (course_code);

CREATE TABLE IF NOT EXISTS hrms_course_enrollments (
    id           BIGSERIAL PRIMARY KEY,
    uuid         UUID        NOT NULL UNIQUE,
    course_id    BIGINT      NOT NULL REFERENCES hrms_training_courses (id),
    staff_id     BIGINT      NOT NULL REFERENCES staff (id),
    enrolled_at  TIMESTAMP   NOT NULL,
    completed_at TIMESTAMP,
    status       VARCHAR(30) NOT NULL DEFAULT 'ENROLLED',
    score        NUMERIC(6,2),
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP   NOT NULL,
    updated_at   TIMESTAMP   NOT NULL,
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255),
    UNIQUE (course_id, staff_id)
);

CREATE INDEX IF NOT EXISTS idx_hrms_course_enrollment_staff
    ON hrms_course_enrollments (staff_id, status);

CREATE TABLE IF NOT EXISTS hrms_course_certificates (
    id             BIGSERIAL PRIMARY KEY,
    uuid           UUID          NOT NULL UNIQUE,
    enrollment_id  BIGINT        NOT NULL UNIQUE REFERENCES hrms_course_enrollments (id),
    cert_title     VARCHAR(300),
    issued_at      DATE,
    expiry_date    DATE,
    object_key     VARCHAR(1000),
    storage_url    VARCHAR(2000),
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL,
    created_by     VARCHAR(255),
    updated_by     VARCHAR(255)
);

