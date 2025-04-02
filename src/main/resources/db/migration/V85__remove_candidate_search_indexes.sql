BEGIN;
-------------------------------------------------------

DROP INDEX IF EXISTS idx_dob_day;
DROP INDEX IF EXISTS idx_dob_month;
DROP INDEX IF EXISTS idx_dob_year;
DROP INDEX IF EXISTS idx_first_name_soundex;
DROP INDEX IF EXISTS idx_last_name_soundex;
DROP INDEX IF EXISTS idx_reference_identifier_type_and_value;

-----------------------------------------------------
COMMIT;