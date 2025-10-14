BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person
    DROP COLUMN sex_code;

-----------------------------------------------------
COMMIT;
