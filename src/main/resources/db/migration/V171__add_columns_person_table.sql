BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person
    RENAME COLUMN sexual_orientation to sexual_orientation_code;

ALTER TABLE IF EXISTS person
    ADD COLUMN interest_to_immigration BOOLEAN NULL,
    ADD COLUMN disability BOOLEAN NULL;

-----------------------------------------------------
COMMIT;