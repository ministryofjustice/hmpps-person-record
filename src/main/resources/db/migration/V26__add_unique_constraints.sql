BEGIN;
-------------------------------------------------------

TRUNCATE TABLE person CASCADE;

ALTER TABLE IF EXISTS person
    ADD CONSTRAINT unique_defendant_id UNIQUE (defendant_id),
    ADD CONSTRAINT unique_crn UNIQUE (crn),
    ADD CONSTRAINT unique_prison_number UNIQUE (prison_number);

-----------------------------------------------------
COMMIT;