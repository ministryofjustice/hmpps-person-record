
BEGIN;

-------------------------------------------------------

DROP TABLE IF EXISTS court_message;

CREATE TABLE IF NOT EXISTS court_message
(
    id                                      SERIAL      PRIMARY KEY,
    message_id                              TEXT        DEFAULT NULL,
    message                                 TEXT        DEFAULT NULL,
    version                                 int4        NOT NULL DEFAULT 0,
    fk_hearing_id                           bigInt      NOT NULL
);
CREATE INDEX idx_court_message_message_id ON court_message(message_id);

ALTER TABLE IF EXISTS court_message add constraint fk_court_hearing_id foreign key (fk_hearing_id) references court_hearing;

-------------------------------------------------------

COMMIT;