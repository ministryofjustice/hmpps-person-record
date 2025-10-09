BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person
    DROP COLUMN birth_country;

ALTER TABLE IF EXISTS address
    DROP COLUMN country_code;

-----------------------------------------------------
COMMIT;
