BEGIN;
-------------------------------------------------------

ALTER TABLE person
    ADD COLUMN currently_managed BOOLEAN NULL;

-----------------------------------------------------
COMMIT;