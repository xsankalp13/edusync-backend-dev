-- Fix sequence generation mismatch duplicates
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.sequences
        WHERE sequence_schema = 'public'
          AND sequence_name = 'audit_logs_id_seq'
    ) THEN
        -- Force sequence to start at the next multiple of 50 after the max current ID
        -- This prevents Hibernate pooled optimizer from generating duplicate IDs
        EXECUTE 'SELECT setval(''audit_logs_id_seq'', (COALESCE((SELECT MAX(id) FROM audit_logs), 0) / 50 + 2) * 50, false)';
    END IF;
END $$;
