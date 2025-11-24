BEGIN;
-------------------------------------------------------

ALTER TABLE personrecordservice.pseudonym
    ADD COLUMN title_code TEXT NULL;

-----------------------------------------------------
COMMIT;