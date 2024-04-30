BEGIN;
-------------------------------------------------------

ALTER TABLE person
    DROP COLUMN IF EXISTS most_recent_prisoner_number;

-----------------------------------------------------
COMMIT;