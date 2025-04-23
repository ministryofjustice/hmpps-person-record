BEGIN;
-------------------------------------------------------
ALTER TABLE IF EXISTS person
    RENAME COLUMN sex to sex_code;
-----------------------------------------------------
COMMIT;