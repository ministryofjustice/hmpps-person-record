BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.prison_religion
    ALTER COLUMN modify_date_time SET NOT NULL,
    ALTER COLUMN modify_user_id SET NOT NULL;

-----------------------------------------------------
COMMIT;