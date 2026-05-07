BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.address
    ADD COLUMN is_verified BOOLEAN NULL;

-----------------------------------------------------
COMMIT;