BEGIN;

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
    created_by                              TEXT        NOT NULL,
    created_date                            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_by                         TEXT        NOT NULL,
    last_updated_date                       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                                 int4        NOT NULL DEFAULT 0

);

DROP TABLE IF EXISTS address_aud;

CREATE TABLE address_aud
(
    rev integer not null,
    revtype smallint,
    id bigint not null,
    primary key (rev, id),
    address_line_one                        TEXT,
    address_line_two                        TEXT,
    address_line_three                      TEXT,
    address_line_four                       TEXT,
    address_line_five                       TEXT,
    postcode                                TEXT
);

alter table if exists address_aud add constraint address_aud_revinfo_FK foreign key (rev) references revinfo;

ALTER TABLE defendant
    DROP COLUMN address_line_one,
    DROP COLUMN address_line_two,
    DROP COLUMN address_line_three,
    DROP COLUMN address_line_four,
    DROP COLUMN address_line_five,
    DROP COLUMN postcode;


ALTER table defendant
    Add column fk_address_id bigInt NULL;

alter table if exists defendant_aud add column fk_address_id bigInt;

ALTER TABLE defendant ADD CONSTRAINT fk_address FOREIGN KEY (fk_address_id) REFERENCES address;


COMMIT;