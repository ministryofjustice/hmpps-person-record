BEGIN;

-------------------------------------------------------

DROP TABLE IF EXISTS court_hearing;

CREATE TABLE IF NOT EXISTS court_hearing
(
    id                                      SERIAL      PRIMARY KEY,
    hearing_id                              TEXT        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0
);
CREATE INDEX idx_court_hearing_hearing_id ON court_hearing(hearing_id);

-------------------------------------------------------

COMMIT;
