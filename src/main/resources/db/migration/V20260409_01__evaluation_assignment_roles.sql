-- =====================================================================
-- V20260409_01  Add role-based separation to evaluation_assignments
-- =====================================================================

-- 1. Add role column (default EVALUATOR for backward compat)
ALTER TABLE evaluation_assignments
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'EVALUATOR';

-- 2. Add upload_status column (only meaningful for UPLOADER role)
ALTER TABLE evaluation_assignments
    ADD COLUMN upload_status VARCHAR(20) DEFAULT NULL;

-- 3. Drop old unique constraint (try both possible names)
ALTER TABLE evaluation_assignments
    DROP CONSTRAINT IF EXISTS uq_evaluation_assignments_schedule_teacher;

ALTER TABLE evaluation_assignments
    DROP CONSTRAINT IF EXISTS uk_evaluation_assignments_schedule_id_teacher_id;

-- Also try Hibernate-style auto-generated names
ALTER TABLE evaluation_assignments
    DROP CONSTRAINT IF EXISTS ukq_evaluation_assignments;

-- 4. Add new unique constraint including role
ALTER TABLE evaluation_assignments
    ADD CONSTRAINT uq_eval_assign_schedule_teacher_role
    UNIQUE (schedule_id, teacher_id, role);

-- 5. Add index for schedule+role queries
CREATE INDEX IF NOT EXISTS idx_eval_assign_schedule_role
    ON evaluation_assignments(schedule_id, role);
