-- Add side configuration for DOUBLE seating schedules.
ALTER TABLE exam_schedule
	ADD COLUMN IF NOT EXISTS seat_side VARCHAR(5);

-- Backfill historical DOUBLE schedules to a deterministic side.
UPDATE exam_schedule
SET seat_side = 'LEFT'
WHERE max_students_per_seat = 2
  AND seat_side IS NULL;

-- Keep values constrained to LEFT/RIGHT when present.
ALTER TABLE exam_schedule
	DROP CONSTRAINT IF EXISTS chk_exam_schedule_seat_side;

ALTER TABLE exam_schedule
	ADD CONSTRAINT chk_exam_schedule_seat_side
	CHECK (seat_side IS NULL OR seat_side IN ('LEFT', 'RIGHT'));

