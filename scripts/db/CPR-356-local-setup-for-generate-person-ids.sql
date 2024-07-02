
-- insert the CRNs with cluster_high. Don't bother populating the unused fields
INSERT into personmatchscore.splink_cluster (source_dataset, cluster_high, crn)
select 'probation_delius' as source_dataset ,	row_number() over (order by CRN) as cluster_high, crn
from personrecordservice.person p
where p.crn  != '';

-- insert the prison numbers with cluster_high
INSERT into personmatchscore.splink_cluster (source_dataset, cluster_high, prison_number)
select 'prison_nomis' as source_dataset ,	row_number() over (order by prison_number) as cluster_high, prison_number
from personrecordservice.person p where p.prison_number  != '';
