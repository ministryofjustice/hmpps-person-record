BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS prison_nationalities
    ADD COLUMN id                                      SERIAL      PRIMARY KEY;

ALTER TABLE IF EXISTS prison_nationalities
    RENAME COLUMN nationality_codes TO nationality_code;

-------------------------------------------------------
COMMIT;