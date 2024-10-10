BEGIN;
----------------------------------------

DROP TABLE IF EXISTS event_logging;
CREATE TABLE IF NOT EXISTS event_logging
(
    id                                      UUID        PRIMARY KEY,
    before_data                             JSONB       DEFAULT NULL,
    processed_data                          JSONB       DEFAULT NULL,
    source_system_id                        TEXT        DEFAULT NULL,
    data source                             TEXT        DEFAULT NULL,
    message_event_type                      TEXT        DEFAULT NULL,
    event_timestamp                         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

----------------------------------------
COMMIT;