BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.address_usage
    ADD COLUMN update_id UUID DEFAULT gen_random_uuid() NOT NULL;

-----------------------------------------------------
COMMIT;