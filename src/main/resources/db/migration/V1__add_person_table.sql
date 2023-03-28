DROP TABLE IF EXISTS person;

CREATE TABLE IF NOT EXISTS person
(
    id                                      SERIAL      PRIMARY KEY,
    person_id                               UUID        NOT NULL,
    pnc_number                              TEXT        DEFAULT NULL,
    crn                                     TEXT        DEFAULT NULL,
    created_by                              TEXT        NOT NULL,
    created_date                            TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_by                         TEXT        NOT NULL,
    last_updated_date                       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                                 int4        NOT NULL DEFAULT 0
);