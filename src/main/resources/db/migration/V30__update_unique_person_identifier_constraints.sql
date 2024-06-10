BEGIN;
-------------------------------------------------------
ALTER TABLE IF EXISTS person
    DROP CONSTRAINT unique_prison_number;

ALTER TABLE IF EXISTS person
    ADD CONSTRAINT unique_prison_number UNIQUE (prison_number, source_system);
-----------------------------------------------------
COMMIT;