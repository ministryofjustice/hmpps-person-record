BEGIN;

DROP TABLE IF EXISTS person_address;

CREATE TABLE IF NOT EXISTS person_address
(
    id                                      SERIAL      PRIMARY KEY,
    postcode                                TEXT        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0,
    fk_person_id                            bigInt      NOT NULL
);
ALTER TABLE IF EXISTS person_address add constraint fk_person_address_id foreign key (fk_person_id) references person;

COMMIT;