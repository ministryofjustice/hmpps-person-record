BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.person
    ADD COLUMN passive_state BOOLEAN NOT NULL DEFAULT FALSE;

-----------------------------------------------------
COMMIT;