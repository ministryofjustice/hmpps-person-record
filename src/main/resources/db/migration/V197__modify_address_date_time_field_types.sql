BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.address
    ALTER COLUMN start_date TYPE TIMESTAMP;

ALTER TABLE IF EXISTS personrecordservice.address
    ALTER COLUMN end_date TYPE TIMESTAMP;

-----------------------------------------------------
COMMIT;