DROP TABLE IF EXISTS person;

CREATE TABLE IF NOT EXISTS person
(
    id                                      SERIAL      PRIMARY KEY,
    person_id                               UUID        NOT NULL,
    created_by                              TEXT        NOT NULL,
    created_date                            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_by                         TEXT        NOT NULL,
    last_updated_date                       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                                 int4        NOT NULL DEFAULT 0
);


CREATE SEQUENCE revinfo_seq START WITH 1 INCREMENT BY 50;

DROP TABLE IF EXISTS revinfo;

CREATE TABLE revinfo
(
    rev INTEGER NOT NULL,
    revtstmp bigint,
    PRIMARY KEY (rev)
);

DROP TABLE IF EXISTS person_aud;

CREATE TABLE person_aud
(
    rev integer not null,
    revtype smallint,
    id bigint not null,
    person_id uuid,
    primary key (rev, id)
);

alter table if exists person_aud add constraint person_aud_revinfo_FK foreign key (rev) references revinfo;


DROP TABLE IF EXISTS defendant;

CREATE TABLE IF NOT EXISTS defendant
(
    id                                      SERIAL      PRIMARY KEY,
    defendant_id                            TEXT        DEFAULT NULL,
    pnc_number                              TEXT        DEFAULT NULL,
    crn                                     TEXT        DEFAULT NULL,
    cro                                     TEXT        DEFAULT NULL,
    title                                   TEXT        DEFAULT NULL,
    forename_one                            TEXT        DEFAULT NULL,
    forename_two                            TEXT        DEFAULT NULL,
    forename_three                          TEXT        DEFAULT NULL,
    surname                                 TEXT        DEFAULT NULL,
    address_line_one                        TEXT        DEFAULT NULL,
    address_line_two                        TEXT        DEFAULT NULL,
    address_line_three                      TEXT        DEFAULT NULL,
    address_line_four                       TEXT        DEFAULT NULL,
    address_line_five                       TEXT        DEFAULT NULL,
    postcode                                TEXT        DEFAULT NULL,
    sex                                     TEXT        DEFAULT NULL,
    nationality_one                         TEXT        DEFAULT NULL,
    nationality_two                         TEXT        DEFAULT NULL,
    date_of_birth                           DATE        DEFAULT NULL,
    created_by                              TEXT        NOT NULL,
    created_date                            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_by                         TEXT        NOT NULL,
    last_updated_date                       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                                 int4        NOT NULL DEFAULT 0,
    fk_person_id                            bigInt      NOT NULL
);

alter table if exists defendant add constraint fk_defendant_person_id foreign key (fk_person_id) references person;

DROP TABLE IF EXISTS defendant_aud;

CREATE TABLE defendant_aud
(
    rev integer not null,
    revtype smallint,
    id bigint not null,
    primary key (rev, id),
    defendant_id                            TEXT,
    pnc_number                              TEXT,
    crn                                     TEXT,
    cro                                     TEXT,
    title                                   TEXT,
    forename_one                            TEXT,
    forename_two                            TEXT,
    forename_three                          TEXT,
    surname                                 TEXT,
    address_line_one                        TEXT,
    address_line_two                        TEXT,
    address_line_three                      TEXT,
    address_line_four                       TEXT,
    address_line_five                       TEXT,
    postcode                                TEXT,
    sex                                     TEXT,
    nationality_one                         TEXT,
    nationality_two                         TEXT,
    date_of_birth                           DATE
);

alter table if exists defendant_aud add constraint defendant_aud_revinfo_FK foreign key (rev) references revinfo;

DROP TABLE IF EXISTS offender;

CREATE TABLE IF NOT EXISTS offender
(
    id                                      SERIAL      PRIMARY KEY,
    crn                                     TEXT        NOT NULL,
    pnc_number                              TEXT        NULL,
    lao                                     BOOLEAN     NULL,
    first_name                              TEXT        NULL,
    last_name                               TEXT        NULL,
    date_of_birth                           DATE        NULL,
    created_by                              TEXT        NOT NULL,
    created_date                            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_by                         TEXT        NOT NULL,
    last_updated_date                       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                                 int4        NOT NULL DEFAULT 0,
    fk_person_id                            bigInt      NOT NULL
);

DROP TABLE IF EXISTS offender_aud;

CREATE TABLE IF NOT EXISTS offender_aud
(
    rev integer not null,
    revtype smallint,
    id bigint not null,
    crn TEXT,
    pnc_number TEXT,
    primary key (rev, id)
);

alter table if exists offender_aud add constraint offender_aud_revinfo_FK foreign key (rev) references revinfo;

DROP TABLE IF EXISTS prisoner;

CREATE TABLE IF NOT EXISTS prisoner
(
    id                                      SERIAL      PRIMARY KEY,
    offender_id                             TEXT        NULL,
    pnc_number                              TEXT        NULL,
    first_name                              TEXT        NULL,
    last_name                               TEXT        NULL,
    date_of_birth                           DATE        NULL,
    created_by                              TEXT        NOT NULL,
    created_date                            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_by                         TEXT        NOT NULL,
    last_updated_date                       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                                 int4        NOT NULL DEFAULT 0,
    fk_person_id                            bigInt      NOT NULL
);

DROP TABLE IF EXISTS prisoner_aud;

CREATE TABLE IF NOT EXISTS prisoner_aud
(
    rev integer not null,
    revtype smallint,
    id bigint not null,
    offender_id TEXT,
    pnc_number TEXT,
    first_name TEXT,
    last_name TEXT,
    date_of_birth DATE,
    primary key (rev, id)
);