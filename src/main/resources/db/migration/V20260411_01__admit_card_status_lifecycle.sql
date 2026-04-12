-- Expand admit-card status lifecycle for async generation pipeline.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_admit_cards_status'
    ) THEN
        ALTER TABLE admit_cards
            DROP CONSTRAINT ck_admit_cards_status;
    END IF;
END
$$;

ALTER TABLE admit_cards
    ADD CONSTRAINT ck_admit_cards_status
    CHECK (status IN ('DRAFT', 'GENERATING', 'GENERATED', 'PUBLISHED', 'FAILED'));

