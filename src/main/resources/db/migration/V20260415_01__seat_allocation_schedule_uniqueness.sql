-- Enforce one allocation per (seat, exam_schedule) while keeping multi-schedule seat sharing.

-- 1) Remove duplicates that would violate the new unique key.
--    Keep the earliest allocation row (smallest id) for each seat+schedule.
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY seat_id, exam_schedule_id ORDER BY id ASC) AS rn
    FROM seat_allocation
)
DELETE FROM seat_allocation sa
USING ranked r
WHERE sa.id = r.id
  AND r.rn > 1;

-- 2) Drop legacy unique constraints if they exist.
ALTER TABLE seat_allocation DROP CONSTRAINT IF EXISTS uk_seat_alloc_seat_schedule_posidx;
ALTER TABLE seat_allocation DROP CONSTRAINT IF EXISTS uk_seat_alloc_seat_schedule_position;

-- 3) Add the new unique constraint on seat+schedule.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_seat_alloc_seat_schedule'
          AND conrelid = 'seat_allocation'::regclass
    ) THEN
        ALTER TABLE seat_allocation
            ADD CONSTRAINT uk_seat_alloc_seat_schedule
            UNIQUE (seat_id, exam_schedule_id);
    END IF;
END $$;

-- 4) Normalize index naming to match new constraint semantics.
DROP INDEX IF EXISTS idx_seat_alloc_posidx;
CREATE INDEX IF NOT EXISTS idx_seat_alloc_seat_schedule
    ON seat_allocation (seat_id, exam_schedule_id, position_index);

