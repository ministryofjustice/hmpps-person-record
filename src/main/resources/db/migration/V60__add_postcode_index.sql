BEGIN;
-----------------------------------------

create index idx_postcode_left on personrecordservice.address (LEFT(postcode, 3));

create index idx_dob_year on personrecordservice.person (date_part('YEAR', date_of_birth));
create index idx_dob_month on personrecordservice.person (date_part('MONTH', date_of_birth));
create index idx_dob_day on personrecordservice.person (date_part('DAY', date_of_birth));

-----------------------------------------
COMMIT;
