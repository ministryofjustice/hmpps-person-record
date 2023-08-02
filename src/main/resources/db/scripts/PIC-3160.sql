/**
  Scripts to run before deploying person record service in to dev,pre-prod and prod.
 */

/**
  Create a person record with existing person id from the defendant table
 */
insert into personrecordservice.person (person_id,created_by,created_date,last_updated_by)
select  distinct on (d.crn) person_id,'migration', current_date, 'migration' from personrecordservice.defendant d where d.crn is not null;
commit;

/**
  Create a distinct offender record with crn from the defendant table
 */
insert into personrecordservice.delius_offender (crn,created_by,created_date,last_updated_by,last_updated_date, fk_person_id)
select  distinct on (d.crn) crn,'migration', current_date, 'migration', current_date, p.id from personrecordservice.defendant d
inner join personrecordservice.person p on p.person_id = d.person_id
and d.crn is not null;
commit;

/**
  create a person record from the duplicated defendant ccs data.
 */
insert into personrecordservice.person (person_id,created_by,created_date,last_updated_by)
select  distinct on (d.name->>'forename1',d.name ->>'surname' ,date_of_birth) person_id,'migration', current_date, 'migration' from personrecordservice.defendant d where d.crn is  null;
commit;

/**
  create a unique defendant record from the ccs data
 */

insert into personrecordservice.hmcts_defendant  (defendant_id , pnc_number, crn, cro, title, forename_one, surname, date_of_birth, sex, nationality_one , nationality_two , address_line_one, address_line_two, address_line_three, address_line_four, address_line_five, postcode,fk_person_id, created_by,created_date,last_updated_by, last_updated_date)
select   distinct on (d.name->>'forename1',d.name ->>'surname' ,date_of_birth) defendant_id , pnc, crn, cro,  d.name ->>'title', d.name->>'forename1',d.name ->>'surname' ,date_of_birth , sex, nationality_1 , nationality_2,  d.address ->>'line1',d.address ->>'line2',d.address ->>'line3',d.address ->>'line4',d.address ->>'line5',d.address ->>'postcode',p.id, 'migration', current_date , 'migration', current_date
from personrecordservice.defendant d
inner join personrecordservice.person p on p.person_id = d.person_id
and d.crn is null;
commit;


drop table personrecordservice.defendant;
commit;