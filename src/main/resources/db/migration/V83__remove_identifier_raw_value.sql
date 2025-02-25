BEGIN;
-------------------------------------------------------


ALTER TABLE reference
    DROP COLUMN identifier_raw_value;

-----------------------------------------------------
COMMIT;