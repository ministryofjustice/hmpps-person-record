BEGIN;
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS prison_immigration_status
(
    id                                      SERIAL      PRIMARY KEY,
    prison_number                           TEXT        NULL,
    interest_to_immigration                 BOOLEAN     NULL,
    start_date                              DATE        NULL,
    end_date                                DATE        NULL,
    record_type                             TEXT        NULL

);
-----------------------------------------------------
COMMIT;