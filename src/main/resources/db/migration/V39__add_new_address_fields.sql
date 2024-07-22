BEGIN;
-------------------------------------------------------

ALTER TABLE address
    ADD COLUMN address_full TEXT NULL,
    ADD COLUMN address_type TEXT NULL;

-----------------------------------------------------
COMMIT;