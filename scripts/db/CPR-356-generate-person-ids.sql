-- LINK RECORDS

-- drop existing materialized view if it exists
drop materialized view if exists personmatchscore.prison_probation_cluster_high cascade;

-- Create a MATERIALIZED view (so the UUIDs do not change)
create materialized view personmatchscore.prison_probation_cluster_high as
select a.crn, b.prison_number,gen_random_uuid() as person_id from personmatchscore.splink_cluster a
join personmatchscore.splink_cluster b on a.cluster_high=b.cluster_high
where a.crn != '' and b.prison_number  !=''
group by a.crn, b.prison_number having count(*) = 1;

-- populate the UUIDS
insert into personrecordservice.personkey (person_id, version)
select person_id,1 from personmatchscore.prison_probation_cluster_high;

-- drop existing materialized view if it exists
drop materialized view if exists personmatchscore.prison_probation_cluster_high_person_id cascade;

-- make another view to make adding the foreign keys easier
create view personmatchscore.prison_probation_cluster_high_person_id as
select ch.crn, ch.prison_number, ch.person_id, pi.id as fk_person_id
from personmatchscore.prison_probation_cluster_high ch
join personrecordservice.personkey pi on ch.person_id=pi.person_id;

-- finally do the updates
update personrecordservice.person
set fk_person_key_id = ch.fk_person_id
    from personmatchscore.prison_probation_cluster_high_person_id ch
where ch.crn = personrecordservice.person.crn;

update personrecordservice.person
set fk_person_key_id = ch.fk_person_id
    from personmatchscore.prison_probation_cluster_high_person_id ch
where ch.prison_number = personrecordservice.person.prison_number;

-- LINK UNASSIGNED RECORDS

-- drop existing materialized view if it exists
drop materialized view if exists personmatchscore.prison_probation_orphaned cascade;

-- assign records a UUID that aren't linked
create materialized view personmatchscore.prison_probation_orphaned as
select p.id as person_id, gen_random_uuid() as person_identifier
from personrecordservice.person p
where fk_person_key_id is null;

-- populate the UUIDS
insert into personrecordservice.personkey (person_id, version)
select person_identifier, 1 from personmatchscore.prison_probation_orphaned;

-- drop existing materialized view if it exists
drop view if exists personmatchscore.prison_probation_orphaned_person_key_id cascade;

-- make another view to make adding the foreign keys easier
create view personmatchscore.prison_probation_orphaned_person_key_id as
select pk.id as person_key_id, o.person_id
from personmatchscore.prison_probation_orphaned o
join personrecordservice.personkey pk
on pk.person_id = o.person_identifier;

-- link unassigned to person key
update personrecordservice.person
set fk_person_key_id = opk.person_key_id
    from personmatchscore.prison_probation_orphaned_person_key_id opk
where personrecordservice.person.id = opk.person_id;