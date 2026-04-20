BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.person_key
    DROP COLUMN merged_to;

-----------------------------------------------------
COMMIT;