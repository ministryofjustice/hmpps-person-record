BEGIN;
-------------------------------------------------------
ALTER TABLE IF EXISTS defendant_alias ADD CONSTRAINT fk_defendant_alias_id foreign key (fk_defendant_id) references defendant;

ALTER table prisoner
    Add column fk_address_id bigInt NULL;

ALTER TABLE prisoner ADD CONSTRAINT fk_address FOREIGN KEY (fk_address_id) REFERENCES address;

ALTER table prisoner
    Add column fk_contact_id bigInt NULL;

ALTER TABLE prisoner ADD CONSTRAINT fk_contact FOREIGN KEY (fk_contact_id) REFERENCES contact;

-------------------------------------------------------
ALTER TABLE prisoner
    ADD COLUMN title TEXT NULL,
    ADD COLUMN middle_name TEXT NULL,
    ADD COLUMN offender_id bigint NULL,
    ADD COLUMN root_offender_id bigint NULL,
    ADD COLUMN cro TEXT NULL,
    ADD COLUMN ni_number TEXT NULL,
    ADD COLUMN driving_license_number TEXT NULL,
    ADD COLUMN birth_place TEXT NULL,
    ADD COLUMN birth_country_code TEXT NULL,
    ADD COLUMN sex_code TEXT NULL,
    ADD COLUMN race_code TEXT NULL;
-----------------------------------------------------
COMMIT;