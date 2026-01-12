BEGIN;
-------------------------------------------------------

CREATE UNIQUE INDEX unq_idx_one_current_per_id
ON personrecordservice.prison_religion (prison_number)
WHERE record_type = 'CURRENT';

-----------------------------------------------------
COMMIT;