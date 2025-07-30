BEGIN;
-------------------------------------------------------

UPDATE title_codes
SET description = 'Reverend'
WHERE id = 6;

UPDATE title_codes
SET description = 'Father'
WHERE id = 7;

UPDATE title_codes
SET description = 'Imam'
WHERE id = 8;

-----------------------------------------------------
COMMIT;