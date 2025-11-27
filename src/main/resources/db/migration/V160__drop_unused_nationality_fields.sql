BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.nationalities
    DROP COLUMN start_date;

ALTER TABLE IF EXISTS personrecordservice.nationalities
    DROP COLUMN end_date;

ALTER TABLE IF EXISTS personrecordservice.nationalities
    DROP COLUMN notes;

-----------------------------------------------------
COMMIT;