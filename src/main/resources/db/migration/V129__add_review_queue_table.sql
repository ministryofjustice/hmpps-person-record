BEGIN;
-------------------------------------------------------

DROP TABLE IF EXISTS review;
CREATE TABLE IF NOT EXISTS review (
   id                                      SERIAL               PRIMARY KEY,
   created_at                              TIMESTAMP NOT NULL   DEFAULT CURRENT_TIMESTAMP,
   resolved_at                             TIMESTAMP            DEFAULT NULL
);

-----------------------------------------------------
COMMIT;
