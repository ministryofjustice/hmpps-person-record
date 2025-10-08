BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person
    RENAME COLUMN birth_country to birth_country_code;

ALTER TABLE IF EXISTS address
    RENAME COLUMN country to country_code;

-----------------------------------------------------
COMMIT;