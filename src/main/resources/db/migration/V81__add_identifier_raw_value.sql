BEGIN;
-------------------------------------------------------


ALTER TABLE reference
    DROP COLUMN raw_pnc;

ALTER TABLE reference
    DROP COLUMN raw_cro;

ALTER TABLE reference
    ADD COLUMN identifier_raw_value TEXT DEFAULT NULL;
-----------------------------------------------------
COMMIT;