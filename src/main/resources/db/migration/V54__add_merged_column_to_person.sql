BEGIN;
-------------------------------------------------------

ALTER TABLE person
    ADD COLUMN merged_to BIGINT NULL;

-----------------------------------------------------
COMMIT;