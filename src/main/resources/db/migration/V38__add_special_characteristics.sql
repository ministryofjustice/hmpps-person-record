BEGIN;
-------------------------------------------------------

ALTER TABLE person
    DROP COLUMN IF EXISTS offender_id,
    ADD COLUMN birth_place TEXT NULL,
    ADD COLUMN nationality TEXT NULL,
    ADD COLUMN religion TEXT NULL,
    ADD COLUMN sexual_orientation TEXT NULL,
    ADD COLUMN birth_country TEXT NULL;

-----------------------------------------------------
COMMIT;