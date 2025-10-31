BEGIN;
-------------------------------------------------------

CREATE TABLE IF NOT EXISTS prison_nationalities
(
    prison_number                           TEXT        NULL,
    nationality_codes                       TEXT        NULL,
    start_date                              DATE        NULL,
    end_date                                DATE        NULL,
    nationality_notes                       TEXT        NULL,
    record_type                             TEXT        NULL

);
-----------------------------------------------------
COMMIT;