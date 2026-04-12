-- =====================================================================
-- V20260409_02  Drop the old auto-generated unique constraint on
--               (schedule_id, teacher_id) that Hibernate named randomly.
-- =====================================================================

DO $$
DECLARE
    _constraint_name TEXT;
BEGIN
    FOR _constraint_name IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.constraint_column_usage ccu
            ON tc.constraint_name = ccu.constraint_name
            AND tc.table_schema = ccu.table_schema
        WHERE tc.table_name = 'evaluation_assignments'
          AND tc.constraint_type = 'UNIQUE'
          AND tc.constraint_name <> 'uq_eval_assign_schedule_teacher_role'
        GROUP BY tc.constraint_name
        HAVING array_agg(ccu.column_name::text ORDER BY ccu.column_name::text)
               = ARRAY['schedule_id', 'teacher_id']
    LOOP
        EXECUTE format('ALTER TABLE evaluation_assignments DROP CONSTRAINT %I', _constraint_name);
        RAISE NOTICE 'Dropped old constraint: %', _constraint_name;
    END LOOP;
END $$;
