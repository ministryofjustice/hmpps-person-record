-- Number of orphaned v parented records by Source System
select parented.source_system, orphaned.no_uuid, parented.has_uuid
from (
	select p1.source_system, count(*) as has_uuid
	from personrecordservice.person p1
	where p1.fk_person_key_id is not null
	group by p1.source_system
) as parented
inner join (
	select p2.source_system, count(*) as no_uuid
	from personrecordservice.person p2
	where p2.fk_person_key_id is null
	group by p2.source_system
) as orphaned
on parented.source_system = orphaned.source_system;

-- NOMIS -> DELIUS linked records
select count(pk.id) AS linked
from personrecordservice.personkey pk
join personrecordservice.person p1
	on pk.id = p1.fk_person_key_id
join personrecordservice.person p2
	on pk.id = p2.fk_person_key_id
where p1.source_system = 'NOMIS'
  and p2.source_system = 'DELIUS';

-- NOMIS -> DELIUS not linked records
select count(*) from personrecordservice.personkey pk
                         join personrecordservice.person prison
                              on pk.id = prison.fk_person_key_id
                                  and prison.source_system = 'NOMIS'
where not exists(select * from personrecordservice.person probation
                 where pk.id = probation.fk_person_key_id
                   and probation.source_system = 'DELIUS');

-- Number of UUIDs vs UUIDs that have >1 record by Source System
SELECT
    subquery.source_system,
    COUNT(DISTINCT subquery.fk_person_key_id) FILTER (WHERE uuid_count > 1) AS uuid_count_with_more_than_one,
    COUNT(DISTINCT subquery.fk_person_key_id) AS total_uuid_count
FROM (
    SELECT
        p.fk_person_key_id,
        source_system,
        COUNT(*) AS uuid_count
    FROM personrecordservice.person p
    GROUP BY source_system, p.fk_person_key_id
) AS subquery
GROUP BY source_system;
