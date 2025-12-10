BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.prison_religion
    ALTER COLUMN change_reason_known TYPE BOOLEAN USING change_reason_known::boolean;

-----------------------------------------------------
COMMIT;