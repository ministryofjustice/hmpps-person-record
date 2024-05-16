BEGIN;
-------------------------------------------------------

ALTER TABLE person
    DROP COLUMN IF EXISTS birth_place,
    DROP COLUMN IF EXISTS birth_country;

ALTER TABLE alias RENAME to name;
ALTER TABLE name
    ADD COLUMN title TEXT NULL,
    ADD COLUMN type TEXT;

ALTER TABLE person
    DROP COLUMN IF EXISTS title,
    DROP COLUMN IF EXISTS first_name,
    DROP COLUMN IF EXISTS middle_names,
    DROP COLUMN IF EXISTS last_name,
    DROP COLUMN IF EXISTS date_of_birth;

-----------------------------------------------------
COMMIT;