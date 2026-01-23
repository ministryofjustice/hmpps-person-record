BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.address
    DROP COLUMN address_type;

-----------------------------------------------------
COMMIT;