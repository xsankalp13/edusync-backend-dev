-- Phase 2B: Multi-Level Approval Engine
-- school_id is nullable for forward-compat (single-tenant mode currently)

CREATE TABLE IF NOT EXISTS hrms_approval_chain_configs (
    id          BIGSERIAL PRIMARY KEY,
    uuid        UUID         NOT NULL UNIQUE,
    school_id   BIGINT,
    action_type VARCHAR(40)  NOT NULL,
    chain_name  VARCHAR(200) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_approval_chain_action_type
    ON hrms_approval_chain_configs (action_type, is_active);

CREATE TABLE IF NOT EXISTS hrms_approval_chain_steps (
    id               BIGSERIAL PRIMARY KEY,
    uuid             UUID         NOT NULL UNIQUE,
    chain_config_id  BIGINT       NOT NULL REFERENCES hrms_approval_chain_configs (id),
    step_order       INTEGER      NOT NULL,
    approver_role    VARCHAR(100) NOT NULL,
    step_label       VARCHAR(200),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP    NOT NULL,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_approval_chain_step_config
    ON hrms_approval_chain_steps (chain_config_id, step_order);

CREATE TABLE IF NOT EXISTS hrms_approval_requests (
    id                   BIGSERIAL PRIMARY KEY,
    uuid                 UUID        NOT NULL UNIQUE,
    school_id            BIGINT,
    action_type          VARCHAR(40) NOT NULL,
    entity_type          VARCHAR(100),
    entity_ref           UUID,
    requested_by_ref     UUID,
    requested_at         TIMESTAMP   NOT NULL,
    current_step_order   INTEGER     NOT NULL DEFAULT 1,
    final_status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at         TIMESTAMP,
    is_active            BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP   NOT NULL,
    updated_at           TIMESTAMP   NOT NULL,
    created_by           VARCHAR(255),
    updated_by           VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_approval_request_status
    ON hrms_approval_requests (final_status, action_type);
CREATE INDEX IF NOT EXISTS idx_hrms_approval_request_entity_ref
    ON hrms_approval_requests (entity_ref);

CREATE TABLE IF NOT EXISTS hrms_approval_step_records (
    id             BIGSERIAL PRIMARY KEY,
    uuid           UUID         NOT NULL UNIQUE,
    request_id     BIGINT       NOT NULL REFERENCES hrms_approval_requests (id),
    step_order     INTEGER      NOT NULL,
    approver_role  VARCHAR(100) NOT NULL,
    approver_ref   UUID,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    remarks        VARCHAR(1000),
    acted_at       TIMESTAMP,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    created_by     VARCHAR(255),
    updated_by     VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_hrms_approval_step_record_request
    ON hrms_approval_step_records (request_id, step_order);

