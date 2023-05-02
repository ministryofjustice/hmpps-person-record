/**
  Scripts to run before deploying person record service in to dev,pre-prod and prod.
 */
insert into personrecordservice.person (person_id,pnc_number,crn,created_by,created_date,last_updated_by ,date_of_birth,family_name,given_name)
select person_id,pnc,crn,'migration', current_date , 'migration', date_of_birth , d.name ->>'surname' , d.name->>'forename1'
from personrecordservice.defendant d;
commit;

drop table personrecordservice.defendant;
commit;