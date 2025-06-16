BEGIN;
-----------------------------------------

ALTER TABLE IF EXISTS person
    DROP COLUMN currently_managed;

-----------------------------------------
COMMIT;