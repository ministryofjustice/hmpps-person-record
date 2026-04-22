BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.address
    ADD COLUMN status_code TEXT NULL;

-----------------------------------------------------
COMMIT;