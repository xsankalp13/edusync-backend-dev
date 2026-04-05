-- Staff category/designation foundation + leave reviewer user-level metadata

CREATE TABLE IF NOT EXISTS hrms_staff_designations (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    designation_code VARCHAR(20) NOT NULL,
    designation_name VARCHAR(100) NOT NULL,
    category VARCHAR(40) NOT NULL,
    description VARCHAR(500),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_hrms_staff_designation_code
    ON hrms_staff_designations (designation_code);

ALTER TABLE staff
    ADD COLUMN IF NOT EXISTS staff_category VARCHAR(40),
    ADD COLUMN IF NOT EXISTS designation_id BIGINT;

-- Backfill existing staff as teaching by default for compatibility.
UPDATE staff
SET staff_category = COALESCE(staff_category, 'TEACHING');

-- Seed baseline designations if missing.
INSERT INTO hrms_staff_designations (
    uuid, designation_code, designation_name, category, sort_order, is_active,
    created_at, updated_at, created_by, updated_by
)
SELECT gen_random_uuid(), x.code, x.name, x.category, x.sort_order, TRUE,
       NOW(), NOW(), 'system', 'system'
FROM (
    VALUES
    ('PRT', 'Primary Teacher', 'TEACHING', 10),
    ('TGT', 'Trained Graduate Teacher', 'TEACHING', 20),
    ('PGT', 'Post Graduate Teacher', 'TEACHING', 30),
    ('HOD', 'Head of Department', 'TEACHING', 40),
    ('VP', 'Vice Principal', 'TEACHING', 50),
    ('PRINCIPAL', 'Principal', 'TEACHING', 60),
    ('SPL_EDU', 'Special Educator', 'TEACHING', 70),
    ('PET', 'Physical Education Teacher', 'TEACHING', 80),
    ('ART_TCH', 'Art / Music / Dance Teacher', 'TEACHING', 90),
    ('LAB_INST', 'Lab Instructor', 'TEACHING', 100),
    ('LIBRARIAN', 'Librarian', 'TEACHING', 110),
    ('COMP_TCH', 'Computer / IT Faculty', 'TEACHING', 120),
    ('OFF_MGR', 'Office Manager', 'NON_TEACHING_ADMIN', 200),
    ('ACCOUNTANT', 'Accountant', 'NON_TEACHING_ADMIN', 210),
    ('CASHIER', 'Cashier', 'NON_TEACHING_ADMIN', 220),
    ('RECEPTION', 'Receptionist', 'NON_TEACHING_ADMIN', 230),
    ('CLERK', 'Clerk / Data Entry Operator', 'NON_TEACHING_ADMIN', 240),
    ('HR_EXEC', 'HR Executive', 'NON_TEACHING_ADMIN', 250),
    ('STORE_KPR', 'Store Keeper', 'NON_TEACHING_ADMIN', 260),
    ('ADM_COUN', 'Admission Counselor', 'NON_TEACHING_ADMIN', 270),
    ('PEON', 'Peon / Office Boy', 'NON_TEACHING_SUPPORT', 300),
    ('SWEEPER', 'Sweeper / Housekeeping', 'NON_TEACHING_SUPPORT', 310),
    ('GUARD', 'Security Guard', 'NON_TEACHING_SUPPORT', 320),
    ('DRIVER', 'Bus Driver', 'NON_TEACHING_SUPPORT', 330),
    ('CONDUCTOR', 'Bus Conductor', 'NON_TEACHING_SUPPORT', 340),
    ('GARDENER', 'Gardener / Mali', 'NON_TEACHING_SUPPORT', 350),
    ('ELECTRIC', 'Electrician', 'NON_TEACHING_SUPPORT', 360),
    ('PLUMBER', 'Plumber', 'NON_TEACHING_SUPPORT', 370),
    ('LAB_ATT', 'Lab Attendant', 'NON_TEACHING_SUPPORT', 380),
    ('IT_SUPPORT', 'IT Support', 'NON_TEACHING_SUPPORT', 390),
    ('NURSE', 'Nurse / Medical Staff', 'NON_TEACHING_SUPPORT', 400),
    ('COOK', 'Cook / Kitchen Staff', 'NON_TEACHING_SUPPORT', 410),
    ('MAINT', 'Maintenance Supervisor', 'NON_TEACHING_SUPPORT', 420),
    ('AYAH', 'Ayah / Nanny', 'NON_TEACHING_SUPPORT', 430)
) AS x(code, name, category, sort_order)
WHERE NOT EXISTS (
    SELECT 1 FROM hrms_staff_designations d WHERE d.designation_code = x.code
);

-- Backfill designation from staff_type where possible.
UPDATE staff s
SET designation_id = d.id
FROM hrms_staff_designations d
WHERE s.designation_id IS NULL
  AND (
      (UPPER(COALESCE(s.staff_type, '')) = 'TEACHER' AND d.designation_code = 'TGT')
      OR (UPPER(COALESCE(s.staff_type, '')) = 'PRINCIPAL' AND d.designation_code = 'PRINCIPAL')
      OR (UPPER(COALESCE(s.staff_type, '')) = 'LIBRARIAN' AND d.designation_code = 'LIBRARIAN')
  );

-- Fallback remaining designations to TGT to keep historical data valid.
UPDATE staff s
SET designation_id = d.id
FROM hrms_staff_designations d
WHERE s.designation_id IS NULL
  AND d.designation_code = 'TGT';

ALTER TABLE staff
    ALTER COLUMN staff_category SET NOT NULL,
    ALTER COLUMN designation_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'staff' AND constraint_name = 'fk_staff_designation'
    ) THEN
        ALTER TABLE staff
            ADD CONSTRAINT fk_staff_designation
                FOREIGN KEY (designation_id) REFERENCES hrms_staff_designations (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_staff_category ON staff(staff_category);
CREATE INDEX IF NOT EXISTS idx_staff_designation_id ON staff(designation_id);

ALTER TABLE hrms_leave_applications
    ADD COLUMN IF NOT EXISTS reviewed_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS reviewed_by_name VARCHAR(200);

-- Keep reviewed_by_name in sync for already reviewed rows where possible.
UPDATE hrms_leave_applications la
SET reviewed_by_name = TRIM(COALESCE(up.first_name, '') || ' ' || COALESCE(up.last_name, ''))
FROM staff s
JOIN user_profiles up ON up.id = s.profile_id
WHERE la.reviewed_by_staff_id = s.id
  AND (la.reviewed_by_name IS NULL OR la.reviewed_by_name = '');

CREATE TABLE IF NOT EXISTS hrms_leave_type_applicable_categories (
    leave_type_id BIGINT NOT NULL,
    category VARCHAR(40) NOT NULL,
    CONSTRAINT fk_hrms_leave_type_category_leave_type
        FOREIGN KEY (leave_type_id) REFERENCES hrms_leave_type_configs (id)
);

CREATE INDEX IF NOT EXISTS idx_hrms_leave_type_category
    ON hrms_leave_type_applicable_categories (leave_type_id, category);

ALTER TABLE hrms_salary_templates
    ADD COLUMN IF NOT EXISTS applicable_category VARCHAR(40);

