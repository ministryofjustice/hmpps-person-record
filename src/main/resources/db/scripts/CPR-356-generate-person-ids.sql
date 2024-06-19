-- empty the person identifier table
delete from  personrecordservice.person_identifier
-- TODO also delete all foreign key references from person table TODO TODO

-- run ./gradlew check to create some CRNs and Prison numbers
CREATE SCHEMA IF NOT EXISTS personmatchscore;
CREATE TABLE IF NOT EXISTS personmatchscore.splink_cluster (
                                                               person_id       BIGINT        DEFAULT 0,
                                                               source_dataset  TEXT        DEFAULT NULL,
                                                               cluster_high    BIGINT        DEFAULT 0,
                                                               cluster_medium  BIGINT        DEFAULT 0,
                                                               crn             TEXT        DEFAULT NULL,
                                                               prisoner_number TEXT        DEFAULT NULL
);

-- insert the CRNs with cluster_high. Don't bother populating the unused fields
INSERT into personmatchscore.splink_cluster (source_dataset, cluster_high, crn)
select 'probation_delius' as source_dataset ,	row_number() over (order by CRN) as cluster_high, crn
from personrecordservice.person p
where p.crn  != '';

-- insert the prison numbers with cluster_high
INSERT into personmatchscore.splink_cluster (source_dataset, cluster_high, prisoner_number)
select 'prison_nomis' as source_dataset ,	row_number() over (order by prison_number) as cluster_high, prison_number
from personrecordservice.person p where p.prison_number  != '';
commit;
-- start here if you are working in preprod or prod
-- then create a view
create view personmatchscore.prison_probation_cluster_high as
select a.crn, b.prisoner_number,gen_random_uuid() as person_id from personmatchscore.splink_cluster a
join personmatchscore.splink_cluster b on a.cluster_high=b.cluster_high
where a.crn != '' and b.prisoner_number  !='';

commit;

-- populate the UUIDS
insert into personrecordservice.person_identifier (person_id, version)
select person_id,1 from personmatchscore.prison_probation_cluster_high;
commit;
