BEGIN;
----------------------------------------

DROP TABLE IF EXISTS event_log;
CREATE TABLE IF NOT EXISTS event_log
(
    id                                      SERIAL      PRIMARY KEY,
    source_system_id                        TEXT        DEFAULT NULL,
    match_id                                UUID        DEFAULT NULL,
    uuid                                    UUID        DEFAULT NULL,
    uuid_status_type                        TEXT        DEFAULT NULL,
    first_name                              TEXT        DEFAULT NULL,
    middle_names                            TEXT        DEFAULT NULL,
    last_name                               TEXT        DEFAULT NULL,
    date_of_birth                           TEXT        DEFAULT NULL,
    first_name_aliases                      TEXT[]      DEFAULT ARRAY[]::text[],
    last_name_aliases                       TEXT[]      DEFAULT ARRAY[]::text[],
    date_of_birth_aliases                   TEXT[]      DEFAULT ARRAY[]::text[],
    postcodes                               TEXT[]      DEFAULT ARRAY[]::text[],
    cros                                    TEXT[]      DEFAULT ARRAY[]::text[],
    pncs                                    TEXT[]      DEFAULT ARRAY[]::text[],
    sentence_dates                          TEXT[]      DEFAULT ARRAY[]::text[],
    override_markers                        TEXT[]      DEFAULT ARRAY[]::text[],
    source_system                           TEXT        DEFAULT NULL,
    event_type                              TEXT        DEFAULT NULL,
    operation_id                            TEXT        DEFAULT NULL,
    record_merged_to                        BIGINT      DEFAULT NULL,
    cluster_composition                     TEXT        DEFAULT NULL,
    event_timestamp                         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

----------------------------------------
COMMIT;