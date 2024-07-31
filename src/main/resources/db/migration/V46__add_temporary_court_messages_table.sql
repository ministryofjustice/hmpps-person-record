BEGIN;
-------------------------------------------------------

DROP TABLE IF EXISTS court_message;

CREATE TABLE IF NOT EXISTS court_message
(
    id                                      SERIAL      PRIMARY KEY,
    message_id                              TEXT        DEFAULT NULL,
    message                                 TEXT        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0
);
CREATE INDEX idx_court_message_message_id ON court_message(message_id);

-------------------------------------------------------

COMMIT;
