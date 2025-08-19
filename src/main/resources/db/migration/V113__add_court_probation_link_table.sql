BEGIN;
-------------------------------------------------------

CREATE TABLE court_probation_link_table(
    defendant_id TEXT UNIQUE NOT NULL,
    crn TEXT UNIQUE NOT NULL
);
CREATE INDEX idx_court_probation_defendant_id ON court_probation_link_table(defendant_id);

-----------------------------------------------------
COMMIT;