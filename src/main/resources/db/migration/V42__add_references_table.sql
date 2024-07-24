BEGIN;
-------------------------------------------------------

DROP TABLE IF EXISTS reference;

CREATE TABLE IF NOT EXISTS reference
(
    id                                      SERIAL      PRIMARY KEY,
    identifier_type                         TEXT        DEFAULT NULL,
    identifier_value                        TEXT        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0,
    fk_person_id                            bigInt      NOT NULL
);
ALTER TABLE IF EXISTS reference add constraint fk_person_reference_id foreign key (fk_person_id) references person;

-------------------------------------------------------
COMMIT;
