BEGIN;
-------------------------------------------------------
UPDATE ethnicity_codes
SET description = 'Black/Black British : Any other background'
WHERE id = 8;

UPDATE ethnicity_codes
SET description = 'White : Irish'
WHERE id = 18;


UPDATE ethnicity_codes
SET description = 'Asian/Asian British : Any other backgr''nd'
WHERE id = 5;

UPDATE ethnicity_codes
SET description = 'Black/Black British : Any other backgr''nd'
WHERE id = 8;

-----------------------------------------------------
COMMIT;