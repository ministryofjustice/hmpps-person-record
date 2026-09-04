BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.address
DROP COLUMN record_type;

-----------------------------------------------------
COMMIT;
