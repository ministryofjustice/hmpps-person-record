BEGIN;

DROP TABLE IF EXISTS person_alias;

CREATE TABLE IF NOT EXISTS person_alias
(
    id                                      SERIAL      PRIMARY KEY,
    first_name                              TEXT        DEFAULT NULL,
    middle_names                            TEXT        DEFAULT NULL,
    last_name                               TEXT        DEFAULT NULL,
    date_of_birth                           DATE        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0,
    fk_person_id                            bigInt      NOT NULL
);
ALTER TABLE IF EXISTS person_alias add constraint fk_person_alias_id foreign key (fk_person_id) references person;

COMMIT;