BEGIN;
-------------------------------------------------------
CREATE UNIQUE INDEX unq_idx_update_id ON personrecordservice.address(update_id);

-----------------------------------------------------
COMMIT;