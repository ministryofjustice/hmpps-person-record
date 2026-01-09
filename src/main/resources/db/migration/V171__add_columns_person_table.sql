BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person
    ADD COLUMN interest_to_immigration BOOLEAN NULL,
    ADD COLUMN disability BOOLEAN NULL;

-----------------------------------------------------
COMMIT;