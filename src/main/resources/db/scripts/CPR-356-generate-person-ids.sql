-- Create a MATERIALIZED view (so the UUIDs do not change)
create materialized view personmatchscore.prison_probation_cluster_high as
select a.crn, b.prison_number,gen_random_uuid() as person_id from personmatchscore.splink_cluster a
join personmatchscore.splink_cluster b on a.cluster_high=b.cluster_high
where a.crn != '' and b.prison_number  !='';



-- populate the UUIDS
insert into personrecordservice.person_identifier (person_id, version)
select person_id,1 from personmatchscore.prison_probation_cluster_high;

-- make another view to make adding the foreign keys easier
create view personmatchscore.prison_probation_cluster_high_person_id as
select ch.crn, ch.prison_number, ch.person_id, pi.id as fk_person_id
from personmatchscore.prison_probation_cluster_high ch
join personrecordservice.person_identifier pi on ch.person_id=pi.person_id;

-- finally do the updates
update personrecordservice.person
set fk_person_identifier_id = ch.fk_person_id
    from personmatchscore.prison_probation_cluster_high_person_id ch
where ch.crn = personrecordservice.person.crn

update personrecordservice.person
set fk_person_identifier_id = ch.fk_person_id
    from personmatchscore.prison_probation_cluster_high_person_id ch
where ch.prison_number = personrecordservice.person.prison_number