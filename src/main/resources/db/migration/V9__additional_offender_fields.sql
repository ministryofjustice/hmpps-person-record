BEGIN;
-------------------------------------------------------

ALTER TABLE IF EXISTS offender ADD CONSTRAINT fk_offender_person_id foreign key (fk_person_id) references person;

ALTER TABLE IF EXISTS prisoner ADD CONSTRAINT fk_prisoner_person_id foreign key (fk_person_id) references person;

ALTER table offender
    Add column fk_address_id bigInt NULL;

ALTER TABLE offender ADD CONSTRAINT fk_address FOREIGN KEY (fk_address_id) REFERENCES address;

ALTER table offender
    Add column fk_contact_id bigInt NULL;

ALTER TABLE offender ADD CONSTRAINT fk_contact FOREIGN KEY (fk_contact_id) REFERENCES contact;

--------- offender alis ------------------
DROP TABLE IF EXISTS offender_alias;

CREATE TABLE IF NOT EXISTS offender_alias
(
    id                                      SERIAL      PRIMARY KEY,
    first_name                              TEXT        DEFAULT NULL,
    middle_name                             TEXT        DEFAULT NULL,
    surname                                 TEXT        DEFAULT NULL,
    date_of_birth                           DATE        DEFAULT NULL,
    alias_offender_id                       TEXT        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0,
    fk_offender_id                          bigint      NOT NULL
);
ALTER TABLE IF EXISTS offender_alias ADD CONSTRAINT fk_offender_alias_id foreign key (fk_offender_id) references offender;

-------------------------------------------------------




ALTER TABLE offender
    ADD COLUMN title TEXT NULL,
    ADD COLUMN offender_id TEXT NULL,
    ADD COLUMN cro TEXT NULL,
    ADD COLUMN most_recent_prison_number TEXT NULL,
    ADD COLUMN nationality TEXT NULL,
    ADD COLUMN ni_number TEXT NULL,
    ADD COLUMN ethnicity TEXT NULL,
    ADD COLUMN preferred_name TEXT NULL,
    ADD COLUMN previous_surname TEXT NULL,
    ADD COLUMN gender TEXT NULL;
-----------------------------------------------------
COMMIT;