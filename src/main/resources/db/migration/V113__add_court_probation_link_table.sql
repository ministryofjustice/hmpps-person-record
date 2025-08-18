BEGIN;
-------------------------------------------------------

CREATE TABLE court_probation_link_table(
    defendant_id TEXT NOT NULL,
    crn TEXT NOT NULL
);
CREATE INDEX idx_court_probation_defendant_id ON court_probation_link_table(defendant_id);

-----------------------------------------------------
COMMIT;