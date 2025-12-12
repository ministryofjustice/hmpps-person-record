BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.prison_religion
    DROP COLUMN cpr_religion_id,
    DROP COLUMN create_date_time,
    DROP COLUMN create_user_id,
    DROP COLUMN create_display_name,
    DROP COLUMN modify_display_name,
    DROP COLUMN status;

-----------------------------------------------------
COMMIT;