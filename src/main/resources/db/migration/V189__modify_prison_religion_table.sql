BEGIN;
-------------------------------------------------------

truncate table personrecordservice.prison_religion;

ALTER TABLE IF EXISTS personrecordservice.prison_religion
    ADD COLUMN IF NOT EXISTS create_user_id TEXT NOT NULL;

ALTER TABLE IF EXISTS personrecordservice.prison_religion
    ADD COLUMN IF NOT EXISTS create_date_time TIMESTAMP NOT NULL;

ALTER TABLE IF EXISTS personrecordservice.prison_religion
    ALTER COLUMN modify_date_time DROP NOT NULL,
    ALTER COLUMN modify_user_id DROP NOT NULL;

-----------------------------------------------------
COMMIT;