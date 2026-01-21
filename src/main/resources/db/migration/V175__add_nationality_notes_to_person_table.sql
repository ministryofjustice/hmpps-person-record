BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person
    ADD COLUMN nationality_notes TEXT NULL;

-----------------------------------------------------
COMMIT;