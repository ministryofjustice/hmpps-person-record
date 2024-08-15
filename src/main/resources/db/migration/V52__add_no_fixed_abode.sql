BEGIN;
-------------------------------------------------------

ALTER TABLE address
    ADD COLUMN no_fixes_abode BOOLEAN NULL,
    ADD COLUMN start_date DATE NULL,
    ADD COLUMN end_date DATE NULL;

-----------------------------------------------------
COMMIT;