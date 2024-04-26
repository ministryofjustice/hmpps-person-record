BEGIN;

DROP TABLE IF EXISTS person;

CREATE TABLE IF NOT EXISTS person
(
    id                                      SERIAL      PRIMARY KEY,
    first_name                              TEXT        DEFAULT NULL,
    middle_names                            TEXT        DEFAULT NULL,
    last_name                               TEXT        DEFAULT NULL,
    pnc                                     TEXT        DEFAULT NULL,
    crn                                     TEXT        DEFAULT NULL,
    prisoner_number                         TEXT        DEFAULT NULL,
    most_recent_prisoner_number             TEXT        DEFAULT NULL,
    offender_id                             TEXT        DEFAULT NULL,
    defendant_id                            TEXT        DEFAULT NULL,
    master_defendant_id                     TEXT        DEFAULT NULL,
    cro                                     TEXT        DEFAULT NULL,
    fingerprint                             BOOLEAN     NOT NULL DEFAULT FALSE,
    national_insurance_number               TEXT        DEFAULT NULL,
    driver_license_number                   TEXT        DEFAULT NULL,
    arrest_summons_number                   TEXT        DEFAULT NULL,
    telephone_number                        TEXT        DEFAULT NULL,
    mobile_number                           TEXT        DEFAULT NULL,
    email_address                           TEXT        DEFAULT NULL,
    date_of_birth                           DATE        DEFAULT NULL,
    birth_place                             TEXT        DEFAULT NULL,
    birth_country                           TEXT        DEFAULT NULL,
    source_system                           int4        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0,
);

COMMIT;