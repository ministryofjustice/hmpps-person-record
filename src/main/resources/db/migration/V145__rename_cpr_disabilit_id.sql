ALTER TABLE IF EXISTS prison_disability_status
    RENAME COLUMN cpr_disability_id TO cpr_disability_status_id;

-----------------------------------------------------
COMMIT;