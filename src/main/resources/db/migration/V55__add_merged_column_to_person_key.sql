BEGIN;
-------------------------------------------------------

ALTER TABLE personkey
    ADD COLUMN merged_to BIGINT NULL;

-----------------------------------------------------
COMMIT;