BEGIN;
-------------------------------------------------------

ALTER TABLE person
    DROP COLUMN IF EXISTS fingerprint;

-----------------------------------------------------
COMMIT;