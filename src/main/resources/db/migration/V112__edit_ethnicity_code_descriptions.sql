BEGIN;
-------------------------------------------------------
UPDATE ethnicity_codes
SET description = 'Asian/Asian British : Chinese'
WHERE code = 'A4';

UPDATE ethnicity_codes
SET description = 'Other : Arab'
WHERE code = 'O2';

UPDATE ethnicity_codes
SET description = 'Other : Any other background'
WHERE code = 'O9';

UPDATE ethnicity_codes
SET description = 'White : Gypsy or Irish Traveller'
WHERE code = 'W3';

UPDATE ethnicity_codes
SET description = 'Black/Black British : Caribbean'
WHERE code = 'B1';

UPDATE ethnicity_codes
SET description = 'Mixed : White and Black Caribbean'
WHERE code = 'M1';

UPDATE ethnicity_codes
SET description = 'White : Gypsy or Irish Traveller'
WHERE code = 'W4';


UPDATE ethnicity_codes
SET description = 'White : Roma'
WHERE code = 'W5';


-----------------------------------------------------
COMMIT;