BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person
    DROP COLUMN title,
    DROP COLUMN date_of_birth,
    DROP COLUMN first_name,
    DROP COLUMN middle_names,
    DROP COLUMN last_name;

-----------------------------------------------------
COMMIT;