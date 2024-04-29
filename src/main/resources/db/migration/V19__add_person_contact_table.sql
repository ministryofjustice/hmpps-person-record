BEGIN;

DROP TABLE IF EXISTS person_contact;

CREATE TABLE IF NOT EXISTS person_contact
(
    id                                      SERIAL      PRIMARY KEY,
    contact_type                            TEXT        DEFAULT NULL,
    contact_value                           TEXT        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0,
    fk_person_id                            bigInt      NOT NULL
);
ALTER TABLE IF EXISTS person_contact add constraint fk_person_contact_id foreign key (fk_person_id) references person;

COMMIT;