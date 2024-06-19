CREATE TABLE IF NOT EXISTS personmatchscore.splink_cluster (
    person_id       BIGINT        DEFAULT 0,
    source_dataset  TEXT        DEFAULT NULL,
    cluster_high    BIGINT        DEFAULT 0,
    cluster_medium  BIGINT        DEFAULT 0,
    crn             TEXT        DEFAULT NULL,
    prisoner_number TEXT        DEFAULT NULL
);
