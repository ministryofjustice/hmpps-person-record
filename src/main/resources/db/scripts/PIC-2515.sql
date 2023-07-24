/**
  Scripts to run before deploying person record service in to dev,pre-prod and prod.
 */
insert into personrecordservice.person (crn,person_id,pnc_number,created_by,created_date,last_updated_by ,date_of_birth,family_name,given_name)
select  distinct on (d.crn) crn , person_id,pnc,'migration', current_date , 'migration', date_of_birth , d.name ->>'surname' , d.name->>'forename1'
from personrecordservice.defendant d where d.crn is not null;
commit;

drop table personrecordservice.defendant;
commit;