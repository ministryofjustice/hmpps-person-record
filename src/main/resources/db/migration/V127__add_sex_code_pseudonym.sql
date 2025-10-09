BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS pseudonym
    ADD COLUMN sex_code;

-----------------------------------------------------
COMMIT;
