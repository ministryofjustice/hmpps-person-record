BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS address
    ADD COLUMN sub_building_name TEXT DEFAULT NULL,
    ADD COLUMN building_name TEXT DEFAULT NULL,
    ADD COLUMN building_number TEXT DEFAULT NULL,
    ADD COLUMN thoroughfare_name TEXT DEFAULT NULL,
    ADD COLUMN dependent_locality TEXT DEFAULT NULL,
    ADD COLUMN post_town TEXT DEFAULT NULL,
    ADD COLUMN county TEXT DEFAULT NULL,
    ADD COLUMN country TEXT DEFAULT NULL,
    ADD COLUMN uprn TEXT DEFAULT NULL;
-----------------------------------------------------
COMMIT;