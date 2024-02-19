BEGIN;
-------- address -------------------------------------------------------
DROP TABLE IF EXISTS address;

CREATE TABLE IF NOT EXISTS address
(
    id                                      SERIAL      PRIMARY KEY,
    address_line_one                        TEXT        DEFAULT NULL,
    address_line_two                        TEXT        DEFAULT NULL,
    address_line_three                      TEXT        DEFAULT NULL,
    address_line_four                       TEXT        DEFAULT NULL,
    address_line_five                       TEXT        DEFAULT NULL,
    postcode                                TEXT        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0

);
-------------------------------------------------------------------

-------- contact -----------------------------------------------------
DROP TABLE IF EXISTS contact;

CREATE TABLE IF NOT EXISTS contact
(
    id                                      SERIAL      PRIMARY KEY,
    home_phone                              TEXT        DEFAULT NULL,
    work_phone                              TEXT        DEFAULT NULL,
    mobile                                  TEXT        DEFAULT NULL,
    primary_email                           TEXT        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0
);
---------------------------------------------------------------------

--------- defendant_alias ------------------
DROP TABLE IF EXISTS defendant_alias;

CREATE TABLE IF NOT EXISTS defendant_alias
(
    id                                      SERIAL      PRIMARY KEY,
    first_name                              TEXT        DEFAULT NULL,
    middle_name                             TEXT        DEFAULT NULL,
    surname                                 TEXT        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0,
    fk_defendant_id                         bigint      NOT NULL
);
-------------------------------------------------------
ALTER TABLE defendant
    DROP COLUMN address_line_one,
    DROP COLUMN address_line_two,
    DROP COLUMN address_line_three,
    DROP COLUMN address_line_four,
    DROP COLUMN address_line_five,
    DROP COLUMN postcode,
    DROP COLUMN created_by,
    DROP COLUMN created_date,
    DROP COLUMN last_updated_by,
    DROP COLUMN last_updated_date;


ALTER table defendant
    Add column fk_address_id bigInt NULL;

ALTER TABLE defendant ADD CONSTRAINT fk_address FOREIGN KEY (fk_address_id) REFERENCES address;

ALTER table defendant
    Add column fk_contact_id bigInt NULL;

ALTER TABLE defendant ADD CONSTRAINT fk_contact FOREIGN KEY (fk_contact_id) REFERENCES contact;

ALTER TABLE offender
    DROP COLUMN created_by,
    DROP COLUMN created_date,
    DROP COLUMN last_updated_by,
    DROP COLUMN last_updated_date;

ALTER TABLE prisoner
    DROP COLUMN created_by,
    DROP COLUMN created_date,
    DROP COLUMN last_updated_by,
    DROP COLUMN last_updated_date;

ALTER TABLE person
    DROP COLUMN created_by,
    DROP COLUMN created_date,
    DROP COLUMN last_updated_by,
    DROP COLUMN last_updated_date;

-----------------------------------------------------
DROP TABLE IF EXISTS defendant_aud;
DROP TABLE IF EXISTS offender_aud;
DROP TABLE IF EXISTS prisoner_aud;
DROP TABLE IF EXISTS person_aud;
DROP TABLE IF EXISTS revinfo;
-----------------------------------------------------
COMMIT;