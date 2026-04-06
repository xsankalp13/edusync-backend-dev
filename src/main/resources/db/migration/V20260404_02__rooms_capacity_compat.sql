-- Compatibility patch for existing ADM room schema.
-- Some existing deployments have `rooms.total_capacity` but not `rooms.capacity`.
-- Hibernate validation now expects `rooms.capacity`.

ALTER TABLE rooms
    ADD COLUMN IF NOT EXISTS capacity INTEGER;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'rooms'
          AND column_name = 'total_capacity'
    ) THEN
        EXECUTE 'UPDATE rooms SET capacity = COALESCE(capacity, total_capacity) WHERE capacity IS NULL';
    END IF;
END $$;

