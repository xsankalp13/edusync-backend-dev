-- 1. Add position column with default for existing rows
ALTER TABLE seat_allocation
    ADD COLUMN position VARCHAR(6) NOT NULL DEFAULT 'SINGLE';

-- 2. Drop old unique constraint
ALTER TABLE seat_allocation
    DROP CONSTRAINT IF EXISTS uk_seat_alloc_seat_schedule_student;

-- 3. Create new unique constraint: no duplicate positions per seat per schedule
ALTER TABLE seat_allocation
    ADD CONSTRAINT uk_seat_alloc_seat_schedule_position
    UNIQUE (seat_id, exam_schedule_id, position);

-- 4. Add index for position lookups
CREATE INDEX idx_seat_alloc_position
    ON seat_allocation (seat_id, exam_schedule_id, position);
