BEGIN;
----------------------------------------

DROP TABLE IF EXISTS override_marker;
CREATE TABLE IF NOT EXISTS override_marker
(
    id                                      SERIAL      PRIMARY KEY,
    marker_type                             TEXT        DEFAULT NULL,
    marker_value                            TEXT        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0,
    fk_person_id                            bigInt      NOT NULL
);
CREATE INDEX idx_override_marker_fk_person_id ON override_marker(fk_person_id);

----------------------------------------
COMMIT;