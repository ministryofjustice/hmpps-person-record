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

drop table personrecordservice.defendant;
commit;