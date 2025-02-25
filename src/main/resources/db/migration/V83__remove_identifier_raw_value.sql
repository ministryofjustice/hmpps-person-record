BEGIN;
-------------------------------------------------------


ALTER TABLE IF EXISTS reference
    DROP COLUMN identifier_raw_value;

-----------------------------------------------------
COMMIT;