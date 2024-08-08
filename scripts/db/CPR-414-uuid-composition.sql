-- Number of orphaned v parented records by Source System
select parented.source_system, orphaned.orphaned_count, parented.parented_count
from (
	select p1.source_system, count(*) as parented_count
	from personrecordservice.person p1
	where p1.fk_person_key_id is not null
	group by p1.source_system
) as parented
inner join (
	select p2.source_system, count(*) as orphaned_count
	from personrecordservice.person p2
	where p2.fk_person_key_id is null
	group by p2.source_system
) as orphaned
on parented.source_system = orphaned.source_system

-- Matched: Number of Nomis records that have delius record vs None
select count(distinct pk.id) AS linked
from personrecordservice.personkey pk
join personrecordservice.person p1 on pk.id = p1.fk_person_key_id
join personrecordservice.person p2 on pk.id = p2.fk_person_key_id
where p1.source_system = 'NOMIS'
  and p2.source_system = 'DELIUS';

