-- Phase 2A: Staff Document Management

CREATE TABLE IF NOT EXISTS hrms_staff_documents (
    id                 BIGSERIAL PRIMARY KEY,
    uuid               UUID          NOT NULL UNIQUE,
    school_id          BIGINT,
    staff_id           BIGINT        NOT NULL REFERENCES staff (id),
    category           VARCHAR(40)   NOT NULL,
    display_name       VARCHAR(255)  NOT NULL,
    original_file_name VARCHAR(500),
    object_key         VARCHAR(1000) NOT NULL,
    storage_url        VARCHAR(2000) NOT NULL,
    content_type       VARCHAR(100),
    size_bytes         BIGINT,
    uploaded_at        TIMESTAMP     NOT NULL,
    expiry_date        DATE,
    is_active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP     NOT NULL,
    updated_at         TIMESTAMP     NOT NULL,
    created_by         VARCHAR(255),
    updated_by         VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_staff_documents_staff
    ON hrms_staff_documents (staff_id, is_active);
CREATE INDEX IF NOT EXISTS idx_hrms_staff_documents_category
    ON hrms_staff_documents (staff_id, category);

