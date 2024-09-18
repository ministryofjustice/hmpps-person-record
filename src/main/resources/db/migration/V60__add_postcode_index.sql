BEGIN;
----------------------------------------

create index idx_postcode_left on personrecordservice.address (LEFT(postcode, 3));

----------------------------------------
COMMIT;