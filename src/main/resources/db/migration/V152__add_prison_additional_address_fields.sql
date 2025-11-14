BEGIN;
-------------------------------------------------------

ALTER TABLE personrecordservice.address
    ADD COLUMN "primary" BOOLEAN NULL;

ALTER TABLE personrecordservice.address
    ADD COLUMN mail BOOLEAN NULL;

-----------------------------------------------------
COMMIT;