BEGIN;
-------------------------------------------------------
ALTER TABLE defendant
    DROP COLUMN forename_two,
    DROP COLUMN forename_three;

ALTER TABLE defendant
    RENAME COLUMN forename_one TO first_name;

ALTER TABLE defendant
    ADD COLUMN middle_name TEXT NULL,
    ADD COLUMN driver_number TEXT NULL,
    ADD COLUMN arrest_summons_number TEXT NULL,
    ADD COLUMN master_defendant_id TEXT NULL,
    ADD COLUMN nationality_code TEXT NULL,
    ADD COLUMN ni_number TEXT NULL,
    ADD COLUMN observed_ethnicity TEXT NULL,
    ADD COLUMN self_defined_ethnicity TEXT NULL;
-----------------------------------------------------
COMMIT;