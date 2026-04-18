-- Flyway migration: add emergency contact columns to student_medical_records
-- Generated: 2026-04-18

ALTER TABLE student_medical_records
    ADD COLUMN emergency_contact_name VARCHAR(120);

ALTER TABLE student_medical_records
    ADD COLUMN emergency_contact_phone VARCHAR(30);

