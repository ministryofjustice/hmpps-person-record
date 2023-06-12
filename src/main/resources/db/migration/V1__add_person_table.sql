DROP TABLE IF EXISTS person;

CREATE TABLE IF NOT EXISTS person
(
    id                                      SERIAL      PRIMARY KEY,
    person_id                               UUID        NOT NULL,
    pnc_number                              TEXT        DEFAULT NULL,
    crn                                     TEXT        DEFAULT NULL,
    given_name                              TEXT        DEFAULT NULL,
    family_name                             TEXT        DEFAULT NULL,
    middle_names                            TEXT        DEFAULT NULL,
    date_of_birth                           DATE        DEFAULT NULL,
    created_by                              TEXT        NOT NULL,
    created_date                            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_by                         TEXT        NOT NULL,
    last_updated_date                       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                                 int4        NOT NULL DEFAULT 0
);

ALTER TABLE person ADD UNIQUE (crn);

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
    date_of_birth date,
    rev integer not null,
    revtype smallint,
    id bigint not null,
    person_id uuid,
    crn varchar(255),
    family_name varchar(255),
    given_name varchar(255),
    middle_names varchar(255),
    pnc_number varchar(255),
    primary key (rev, id)
);

alter table if exists person_aud add constraint person_aud_revinfo_FK foreign key (rev) references revinfo;