/**
  Scripts to run before deploying person record service in to dev,pre-prod and prod.
 */

/**
  Delete null and empty pnc records
 */
delete from hmcts_defendant hd where hd.pnc is null or hd.pnc = '' ;

/**
  Create a person records for an unique pnc.
 */
insert into
    personrecordservice.person (person_id,
                                created_by,
                                created_date,
                                last_updated_by)
select distinct on
        (d.pnc) person_id,
               'migration',
               current_date,
               'migration'
from
    personrecordservice.hmcts_defendant d
where
    d.crn is not null;
commit;

/**
  Create a distinct offender record with unique crn and pnc.
 */
insert into
    personrecordservice.offender (crn,
                                  pnc_number,
                                  first_name,
                                  last_name,
                                  date_of_birth,
                                  created_by,
                                  created_date,
                                  last_updated_by,
                                  last_updated_date,
                                  fk_person_id)
select distinct on (d.pnc)
               crn,
               pnc,
               d.name->>'forename1',
               d.name ->>'surname' ,
               date_of_birth ,
               'migration',
               current_date,
               'migration',
               current_date,
               p.id
from
    personrecordservice.hmcts_defendant d
        inner join personrecordservice.person p on
                p.person_id = d.person_id
            and d.crn is not null;

commit;

/**
  Create distinct defendant records with unique crn and pnc.
 */
insert
into
    personrecordservice.defendant (defendant_id ,
                                   pnc_number,
                                   crn,
                                   cro,
                                   title,
                                   forename_one,
                                   surname,
                                   date_of_birth,
                                   sex,
                                   nationality_one ,
                                   nationality_two ,
                                   address_line_one,
                                   address_line_two,
                                   address_line_three,
                                   address_line_four,
                                   address_line_five,
                                   postcode,
                                   fk_person_id,
                                   created_by,
                                   created_date,
                                   last_updated_by,
                                   last_updated_date)
select
    distinct on
        (d.pnc) defendant_id ,
               pnc,
               crn,
               cro,
               d.name ->>'title',
               d.name->>'forename1',
               d.name ->>'surname' ,
               date_of_birth ,
               sex,
               nationality_1 ,
               nationality_2,
               d.address ->>'line1',
               d.address ->>'line2',
               d.address ->>'line3',
               d.address ->>'line4',
               d.address ->>'line5',
               d.address ->>'postcode',
               p.id,
               'migration',
               current_date ,
               'migration',
               current_date
from
    personrecordservice.hmcts_defendant d
        inner join personrecordservice.person p on
                p.person_id = d.person_id
            and d.crn notnull;

commit;

/**
  create a person record from non crn based defendant ccs data.
 */
/*insert into personrecordservice.person (person_id,created_by,created_date,last_updated_by)
select  distinct on (d.name->>'forename1',d.name ->>'surname' ,date_of_birth,pnc) person_id,'migration', current_date, 'migration' from personrecordservice.hmcts_defendant d where d.crn is  null or d.crn = '';
commit;*/

/**
  create a unique defendant records from non crn based defendant records.
 */
/*
insert into personrecordservice.defendant  (defendant_id , pnc_number, crn, cro, title, forename_one, surname, date_of_birth, sex, nationality_one , nationality_two , address_line_one, address_line_two, address_line_three, address_line_four, address_line_five, postcode,fk_person_id, created_by,created_date,last_updated_by, last_updated_date)
select   distinct on (d.name->>'forename1',d.name ->>'surname' ,date_of_birth,pnc) defendant_id , pnc, crn, cro,  d.name ->>'title', d.name->>'forename1',d.name ->>'surname' ,date_of_birth , sex, nationality_1 , nationality_2,  d.address ->>'line1',d.address ->>'line2',d.address ->>'line3',d.address ->>'line4',d.address ->>'line5',d.address ->>'postcode',p.id, 'migration', current_date , 'migration', current_date
from personrecordservice.hmcts_defendant d
inner join personrecordservice.person p on p.person_id = d.person_id
and d.crn is null or d.crn = '';
commit;*/

/*
drop table personrecordservice.hmcts_defendant;
commit;*/