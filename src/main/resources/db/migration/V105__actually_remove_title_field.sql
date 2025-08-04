BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS pseudonym
    DROP COLUMN title;

-----------------------------------------------------
COMMIT;