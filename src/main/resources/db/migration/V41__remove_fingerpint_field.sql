BEGIN;
-------------------------------------------------------

ALTER TABLE person
    DELETE COLUMN IF EXISTS fingerprint;

-----------------------------------------------------
COMMIT;