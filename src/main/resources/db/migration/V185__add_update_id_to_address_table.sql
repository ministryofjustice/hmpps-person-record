BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.address
    ADD COLUMN update_id UUID DEFAULT gen_random_uuid() NOT NULL;

-----------------------------------------------------
COMMIT;