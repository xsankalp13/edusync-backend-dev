-- Hibernate 6 auto-generated CHECK constraints on enum columns (status) for payroll tables
-- were created during ddl-auto:update without the VOIDED value, because VOIDED was added later.
-- This migration drops those stale constraints so the void payroll run operation works correctly.
-- After this migration runs, Hibernate's ddl-auto:update will recreate the constraints
-- including the new VOIDED value on next application start.

DO $$
DECLARE
    r RECORD;
BEGIN
    -- Drop any check constraints on hrms_payroll_runs that reference status values
    FOR r IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints tc
        INNER JOIN information_schema.check_constraints cc
            ON  tc.constraint_name   = cc.constraint_name
            AND tc.constraint_catalog = cc.constraint_catalog
            AND tc.constraint_schema  = cc.constraint_schema
        WHERE tc.table_name       = 'hrms_payroll_runs'
          AND tc.constraint_type  = 'CHECK'
          AND (cc.check_clause ILIKE '%status%' OR cc.check_clause ILIKE '%DRAFT%' OR cc.check_clause ILIKE '%PROCESSED%')
    LOOP
        EXECUTE format('ALTER TABLE hrms_payroll_runs DROP CONSTRAINT IF EXISTS %I', r.constraint_name);
    END LOOP;

    -- Drop any check constraints on hrms_payslips that reference status values
    FOR r IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints tc
        INNER JOIN information_schema.check_constraints cc
            ON  tc.constraint_name   = cc.constraint_name
            AND tc.constraint_catalog = cc.constraint_catalog
            AND tc.constraint_schema  = cc.constraint_schema
        WHERE tc.table_name       = 'hrms_payslips'
          AND tc.constraint_type  = 'CHECK'
          AND (cc.check_clause ILIKE '%status%' OR cc.check_clause ILIKE '%DRAFT%' OR cc.check_clause ILIKE '%PROCESSED%')
    LOOP
        EXECUTE format('ALTER TABLE hrms_payslips DROP CONSTRAINT IF EXISTS %I', r.constraint_name);
    END LOOP;
END;
$$;

-- Explicitly add constraints with the full set of valid values including VOIDED
-- This ensures correct behaviour even if Hibernate does not run ddl-auto:update on the next boot.
ALTER TABLE hrms_payroll_runs
    ADD CONSTRAINT ck_hrms_payroll_runs_status
    CHECK (status IN ('DRAFT','PROCESSING','PROCESSED','APPROVED','DISBURSED','FAILED','VOIDED'));

ALTER TABLE hrms_payslips
    ADD CONSTRAINT ck_hrms_payslips_status
    CHECK (status IN ('DRAFT','PROCESSING','PROCESSED','APPROVED','DISBURSED','FAILED','VOIDED'));
