BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS address
    DROP CONSTRAINT unique_delius_address_id;

-----------------------------------------------------
COMMIT;