BEGIN;
-------------------------------------------------------

ALTER TABLE person
    DROP COLUMN IF EXISTS birth_place,
    DROP COLUMN IF EXISTS birth_country;

ALTER TABLE alias RENAME to name;
ALTER TABLE name ADD COLUMN type TEXT;

-----------------------------------------------------
COMMIT;