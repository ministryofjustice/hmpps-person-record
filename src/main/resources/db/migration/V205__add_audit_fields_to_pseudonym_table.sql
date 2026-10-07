BEGIN;

ALTER TABLE personrecordservice.pseudonym
    ADD COLUMN create_date_time TIMESTAMP NULL,
    ADD COLUMN create_user_id TEXT NULL,
    ADD COLUMN modify_date_time TIMESTAMP NULL,
    ADD COLUMN modify_user_id TEXT NULL;

COMMIT;
