BEGIN;
----------------------------------------
CREATE SCHEMA IF NOT EXISTS personmatchscore;

DROP TABLE IF EXISTS personmatchscore.pnc_frequency;
CREATE TABLE IF NOT EXISTS personmatchscore.pnc_frequency
(
    id                                      SERIAL      PRIMARY KEY,
    pnc                                     TEXT        DEFAULT NULL,
    frequency                               NUMERIC     DEFAULT NULL
);
CREATE INDEX idx_pnc_frequency_pnc ON personmatchscore.pnc_frequency(pnc);

----------------------------------------
COMMIT;