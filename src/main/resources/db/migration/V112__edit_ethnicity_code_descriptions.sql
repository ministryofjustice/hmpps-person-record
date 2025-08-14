BEGIN;
-------------------------------------------------------
UPDATE ethnicity_codes
SET description = 'Asian/Asian British : Chinese'
WHERE id = 4;

UPDATE ethnicity_codes
SET description = 'Other : Arab'
WHERE id = 15;

UPDATE ethnicity_codes
SET description = 'Other : Any other background'
WHERE id = 16;

UPDATE ethnicity_codes
SET description = 'White : Gypsy or Irish Traveller'
WHERE id = 16;

UPDATE ethnicity_codes
SET description = 'Black/Black British : Caribbean'
WHERE id = 6;

UPDATE ethnicity_codes
SET description = 'Mixed : White and Black Caribbean'
WHERE id = 9;

UPDATE ethnicity_codes
SET description = 'Other : Any other background'
WHERE id = 16;

UPDATE ethnicity_codes
SET description = 'White : Gypsy or Irish Traveller'
WHERE id = 19;

UPDATE ethnicity_codes
SET description = 'White : Gypsy or Irish Traveller'
WHERE id = 20;

UPDATE ethnicity_codes
SET description = 'White : Roma'
WHERE id = 21;


-----------------------------------------------------
COMMIT;