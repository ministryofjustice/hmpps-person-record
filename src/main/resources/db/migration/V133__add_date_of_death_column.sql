BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person
    ADD COLUMN date_of_death DATE DEFAULT NULL;
-----------------------------------------------------
COMMIT;