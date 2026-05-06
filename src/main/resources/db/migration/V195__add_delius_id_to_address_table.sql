BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.address
    ADD COLUMN delius_address_id BIGINT NULL;

-----------------------------------------------------
COMMIT;