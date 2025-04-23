BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person
    ADD COLUMN sex_description TEXT DEFAULT NULL;
-----------------------------------------------------
COMMIT;