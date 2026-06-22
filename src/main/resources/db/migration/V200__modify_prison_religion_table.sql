BEGIN;
-------------------------------------------------------

truncate table personrecordservice.prison_religion;

ALTER TABLE IF EXISTS personrecordservice.prison_religion
    ALTER COLUMN religion_code SET NOT NULL,
    ALTER COLUMN start_date SET NOT NULL,
    ALTER COLUMN change_reason_known SET NOT NULL;

CREATE INDEX idx_prison_religion_prison_number
    ON personrecordservice.prison_religion (prison_number);

CREATE UNIQUE INDEX unq_idx_prison_religion_update_id
    ON personrecordservice.prison_religion (update_id);

-----------------------------------------------------
COMMIT;
