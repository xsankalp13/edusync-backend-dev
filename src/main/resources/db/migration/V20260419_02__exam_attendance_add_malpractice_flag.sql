ALTER TABLE exam_attendance
    ADD COLUMN IF NOT EXISTS malpractice_reported BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill legacy rows where MALPRACTICE was stored as status only.
UPDATE exam_attendance
SET malpractice_reported = TRUE,
    status = 'PRESENT'
WHERE status = 'MALPRACTICE';


