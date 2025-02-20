BEGIN;
-------------------------------------------------------


ALTER TABLE reference
     ADD COLUMN raw_pnc TEXT DEFAULT NULL;

ALTER TABLE reference
     ADD COLUMN raw_cro TEXT DEFAULT NULL;

-----------------------------------------------------
COMMIT;