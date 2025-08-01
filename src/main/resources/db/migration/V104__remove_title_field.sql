BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS psuedonym
    DROP COLUMN title;

-----------------------------------------------------
COMMIT;