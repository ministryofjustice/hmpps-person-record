BEGIN;
----------------------------------------

DROP TABLE IF EXISTS event_logging;
CREATE TABLE IF NOT EXISTS event_logging
(
    id                                      SERIAL      PRIMARY KEY,
    operation_id                            TEXT        DEFAULT NULL,
    before_data                             TEXT        DEFAULT NULL,
    processed_data                          TEXT        DEFAULT NULL,
    source_system_id                        TEXT        DEFAULT NULL,
    uuid                                    TEXT        DEFAULT NULL,
    source_system                           TEXT        DEFAULT NULL,
    event_type                              TEXT        DEFAULT NULL,
    event_timestamp                         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

----------------------------------------
COMMIT;
