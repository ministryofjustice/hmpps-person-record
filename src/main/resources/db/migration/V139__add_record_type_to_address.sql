BEGIN;
-------------------------------------------------------

ALTER TABLE personrecordservice.address
    ADD COLUMN record_type TEXT NULL;

-----------------------------------------------------
COMMIT;