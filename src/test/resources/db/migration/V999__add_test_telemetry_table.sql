BEGIN;

DROP TABLE IF EXISTS telemetry;

CREATE TABLE IF NOT EXISTS telemetry
(
    id                                      SERIAL      PRIMARY KEY,
    event                                   TEXT        DEFAULT NULL,
    properties                              JSON        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0
);

COMMIT;