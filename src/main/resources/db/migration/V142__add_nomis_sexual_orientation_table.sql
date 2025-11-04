BEGIN;
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS prison_sexual_orientation
(
    id                                      SERIAL      PRIMARY KEY,
    prison_number                           TEXT        NULL,
    sexual_orientation_code                 TEXT        NULL,
    start_date                              DATE        NULL,
    end_date                                DATE        NULL,
    record_type                             TEXT        NULL
);

-----------------------------------------------------
COMMIT;