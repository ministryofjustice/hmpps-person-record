BEGIN;
-------------------------------------------------------

ALTER TABLE nationalities
    ALTER COLUMN fk_nationality_code_id DROP NOT NULL;

ALTER TABLE nationalities
    DROP CONSTRAINT fk_person_nationalities_id;

ALTER TABLE IF EXISTS nationalities
    ADD constraint fk_person_nationality_id foreign key (fk_person_id) references person ON DELETE CASCADE;

-----------------------------------------------------
COMMIT;