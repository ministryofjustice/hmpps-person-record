BEGIN;
-------------------------------------------------------

ALTER TABLE address
    RENAME COLUMN no_fixes_abode TO no_fixed_abode;

-----------------------------------------------------
COMMIT;