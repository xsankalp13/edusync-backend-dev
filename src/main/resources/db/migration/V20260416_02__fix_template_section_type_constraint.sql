-- Fix template_section.section_type check constraint to match backend enum values.
-- Production-safe notes:
-- 1) Normalize legacy values before adding check (no row loss).
-- 2) Drop/add check constraint idempotently.

UPDATE template_section
SET section_type = 'NORMAL'
WHERE section_type IS NULL OR section_type NOT IN ('NORMAL', 'OPTIONAL');

ALTER TABLE template_section
    DROP CONSTRAINT IF EXISTS template_section_section_type_check;

ALTER TABLE template_section
    ADD CONSTRAINT template_section_section_type_check
    CHECK (section_type IN ('NORMAL', 'OPTIONAL'));


