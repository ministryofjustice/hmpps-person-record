BEGIN;
-------------------------------------------------------

ALTER TABLE alias RENAME TO pseudonym;

ALTER TABLE pseudonym
    ADD COLUMN name_type TEXT NULL,
    ADD COLUMN sex TEXT NULL,
    ADD COLUMN title TEXT NULL,
    ADD COLUMN ethnicity TEXT NULL;

ALTER TABLE person
    ADD COLUMN sex TEXT NULL,
    ADD COLUMN ethnicity TEXT NULL;

-----------------------------------------------------
COMMIT;