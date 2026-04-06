-- Compatibility patch for audit log ID generation.
-- Hibernate expects `audit_logs_id_seq` because AuditLog uses SEQUENCE strategy.

CREATE SEQUENCE IF NOT EXISTS audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    CACHE 50;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'audit_logs'
    ) THEN
        EXECUTE 'SELECT setval(''audit_logs_id_seq'', COALESCE((SELECT MAX(id) FROM audit_logs), 0) + 1, false)';
    END IF;
END $$;

