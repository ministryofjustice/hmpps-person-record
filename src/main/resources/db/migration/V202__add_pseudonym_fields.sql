BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS pseudonym
    ADD COLUMN birth_place TEXT NULL,
    ADD COLUMN birth_country_code TEXT NULL,
    ADD COLUMN sexual_orientation_code TEXT NULL,
    ADD COLUMN ethnicity_code TEXT NULL;


-----------------------------------------------------
COMMIT;