ALTER TABLE sections
    ADD COLUMN IF NOT EXISTS class_teacher_id BIGINT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_sections_class_teacher'
          AND table_name = 'sections'
    ) THEN
        ALTER TABLE sections
            ADD CONSTRAINT fk_sections_class_teacher
            FOREIGN KEY (class_teacher_id) REFERENCES staff(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_sections_class_teacher
    ON sections(class_teacher_id);

