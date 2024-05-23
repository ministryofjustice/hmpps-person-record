BEGIN;
-------------------------------------------------------

ALTER TABLE person
    DROP COLUMN IF EXISTS birth_place,
    DROP COLUMN IF EXISTS birth_country;

-----------------------------------------------------
COMMIT;