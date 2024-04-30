BEGIN;
-------------------------------------------------------

ALTER TABLE person
    RENAME COLUMN prisoner_number TO prison_number;

ALTER TABLE person
    ADD COLUMN title TEXT NULL;
-----------------------------------------------------
COMMIT;