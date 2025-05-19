# 002 - Link Cluster Data

## Importing Data
1) Get CSV file from Robin
2) Using Import Wizard to create temp table 'temp_cluster_info', ensure it contains uuid & match_id columns

## Creating UUIDs
1) Insert unique UUIDS into person_key, run this command:<br />
```
INSERT INTO person_key (person_uuid) SELECT distinct cluster_uuid from temp_uuids;
```

2) Link the person to UUID, run this command: <br />
```
UPDATE person
SET fk_person_key_id = pk.id
FROM person_key pk
JOIN temp_uuids tu ON pk.person_uuid = tu.cluster_uuid
WHERE person.match_id = tu.match_id;
```

3) Update the event log UUID value & status <br />
```
UPDATE event_log as el
SET uuid = pk.person_uuid, uuid_status_type = pk.status
FROM person_key pk
JOIN person p ON pk.id = p.fk_person_key_id
WHERE p.match_id = el.match_id and el.event_type = 'CPR_RECORD_SEEDED';
```

## Delete Temp table
1) Remove the temp table, run: <br />
```
DROP table IF EXISTS temp_uuids
```

## Reporting on cluster data

Copy and paste the SQL from  [CPR-414-uuid-composition-report.sql](../scripts/db/CPR-414-uuid-composition-report.sql) and execute it
