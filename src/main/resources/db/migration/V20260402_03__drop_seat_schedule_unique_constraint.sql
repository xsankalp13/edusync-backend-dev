-- Migration to drop the unique constraint on (seat_id, exam_schedule_id) in seat_allocation
DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT conname INTO constraint_name
    FROM pg_constraint
    WHERE conrelid = 'seat_allocation'::regclass
      AND contype = 'u'
      AND conkey = (
        SELECT array_agg(attnum ORDER BY attnum)
        FROM pg_attribute
        WHERE attrelid = 'seat_allocation'::regclass
          AND attname IN ('seat_id', 'exam_schedule_id')
      );
    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE seat_allocation DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

