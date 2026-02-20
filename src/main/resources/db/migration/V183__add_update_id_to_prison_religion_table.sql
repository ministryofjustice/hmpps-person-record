BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.prison_religion
    ADD COLUMN update_id UUID DEFAULT gen_random_uuid() NOT NULL;

-----------------------------------------------------
COMMIT;