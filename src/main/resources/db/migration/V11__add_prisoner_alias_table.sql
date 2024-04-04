BEGIN;

DROP TABLE IF EXISTS prisoner_alias;

CREATE TABLE IF NOT EXISTS prisoner_alias
(
    id                                      SERIAL      PRIMARY KEY,
    first_name                              TEXT        DEFAULT NULL,
    middle_name                             TEXT        DEFAULT NULL,
    last_name                               TEXT        DEFAULT NULL,
    date_of_birth                           DATE        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0,
    fk_prisoner_id                          bigint      NOT NULL
);
ALTER TABLE IF EXISTS prisoner_alias ADD CONSTRAINT fk_prisoner_alias_id foreign key (fk_prisoner_id) references prisoner;

COMMIT;