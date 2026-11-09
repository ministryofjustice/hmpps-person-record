BEGIN;
-------------------------------------------------------
ALTER TABLE IF EXISTS personrecordservice.reference
    ADD COLUMN is_verified BOOLEAN NULL,
    ADD COLUMN create_date_time TIMESTAMP NULL,
    ADD COLUMN create_user_id TEXT NULL,
    ADD COLUMN modify_date_time TIMESTAMP NULL,
    ADD COLUMN modify_user_id TEXT NULL;

-----------------------------------------------------
COMMIT;
