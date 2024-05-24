BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS person
    DROP COLUMN telephone_number,
    DROP COLUMN mobile_number,
    DROP COLUMN email_address;

-----------------------------------------------------
COMMIT;