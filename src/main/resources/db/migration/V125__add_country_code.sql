BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person
    ADD COLUMN birth_country_code TEXT NULL;

ALTER TABLE IF EXISTS address
    ADD COLUMN country_code TEXT NULL;

-----------------------------------------------------
COMMIT;
