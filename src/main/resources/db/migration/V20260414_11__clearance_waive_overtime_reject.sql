-- Add waived flag to exit clearance items
ALTER TABLE hrms_exit_clearance_items
    ADD COLUMN IF NOT EXISTS waived        BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS waived_by     VARCHAR(200),
    ADD COLUMN IF NOT EXISTS waived_at     TIMESTAMP;

-- Add rejected_at to overtime records (the status column already holds REJECTED)
ALTER TABLE hrms_overtime_records
    ADD COLUMN IF NOT EXISTS rejected_at   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rejection_remarks VARCHAR(500);
