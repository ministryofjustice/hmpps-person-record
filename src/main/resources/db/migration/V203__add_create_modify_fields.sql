BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.address
    ADD COLUMN create_date_time TIMESTAMP NULL,
    ADD COLUMN create_user_id   TEXT      NULL,
    ADD COLUMN modify_date_time TIMESTAMP NULL,
    ADD COLUMN modify_user_id   TEXT      NULL;

ALTER TABLE IF EXISTS personrecordservice.address_usage
    ADD COLUMN create_date_time TIMESTAMP NULL,
    ADD COLUMN create_user_id   TEXT      NULL,
    ADD COLUMN modify_date_time TIMESTAMP NULL,
    ADD COLUMN modify_user_id   TEXT      NULL;

ALTER TABLE IF EXISTS personrecordservice.contact
    ADD COLUMN create_date_time TIMESTAMP NULL,
    ADD COLUMN create_user_id   TEXT      NULL,
    ADD COLUMN modify_date_time TIMESTAMP NULL,
    ADD COLUMN modify_user_id   TEXT      NULL;

-----------------------------------------------------
COMMIT;
