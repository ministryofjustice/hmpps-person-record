BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS personrecordservice.address
    DROP COLUMN "primary";

ALTER TABLE IF EXISTS personrecordservice.address
    DROP COLUMN mail;

-----------------------------------------------------
COMMIT;