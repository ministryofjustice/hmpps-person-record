BEGIN;
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS prison_disability_status
(
    id                                      SERIAL      PRIMARY KEY,
    prison_number                           TEXT        NULL,
    disability                              BOOLEAN     NULL,
    start_date                              DATE        NULL,
    end_date                                DATE        NULL,
    record_type                             TEXT        NULL

);
-----------------------------------------------------
COMMIT;