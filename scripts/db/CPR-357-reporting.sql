-- total number of person records with a UUID
select count (*) from personrecordservice.person where fk_person_identifier_id is not null;

-- total number of UUIDs
select count(*) from personrecordservice.person_identifier;

-- records with a UUID by source system
select source_system, count(fk_person_identifier_id) from personrecordservice.person p1
where p1.fk_person_identifier_id is not null
group by p1.source_system;

-- records with no UUID by source system
select source_system, count(*) from personrecordservice.person p1
where p1.fk_person_identifier_id is null
group by p1.source_system;

-- linked records
select count(*) from personrecordservice.person_identifier pi
                         join personrecordservice.person probation
                              on pi.id = probation.fk_person_identifier_id
                                  and probation.source_system = 'DELIUS'
                         join personrecordservice.person prison
                              on pi.id = prison.fk_person_identifier_id
                                  and prison.source_system = 'NOMIS';

-- UUID on Probation record only
select count(*) from personrecordservice.person_identifier pi
                         join personrecordservice.person probation
                              on pi.id = probation.fk_person_identifier_id
                                  and probation.source_system = 'DELIUS'
where not exists(select * from personrecordservice.person prison
                 where pi.id = prison.fk_person_identifier_id
                   and prison.source_system = 'NOMIS');

-- UUID on Prison record only
select count(*) from personrecordservice.person_identifier pi
                         join personrecordservice.person prison
                              on pi.id = prison.fk_person_identifier_id
                                  and prison.source_system = 'NOMIS'
where not exists(select * from personrecordservice.person probation
                 where pi.id = probation.fk_person_identifier_id
                   and probation.source_system = 'DELIUS');
