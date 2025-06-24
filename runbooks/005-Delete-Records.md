# 005 - Delete Person Records

This runbook outlines the steps to delete Person Records. This can only be done for Probation records at the moment. 

Once deleted, the records cannot be recovered (short of a database restore). So be careful.

Note that any records which have been merged into the record you are deleting will also be deleted.

A log will be kept of the records which have been deleted in the `event_log` table, and AppInsight events are recorded for successful deletions

## Dependencies

* You need a database connection to the read-replica database of `hmpps-person-record`. To do this see:
  * [Connecting to the database](001-Connecting-To-The-Database.md)

* Data will be deleted from the `hmpps-person-match` database as part of this process
  * [hmpps-person-match](https://github.com/ministryofjustice/hmpps-person-match)

## Table of contents

1. [Identify records to delete](#1-identify-records-to-delete)
2. [Delete records](#2-delete-records)
3. [Check deletion worked as expected](#3-check-deletion-worked-as-expected)

## 1. Identify records to delete

You should have a very good reason for doing this. It is likely that you are here because some data has incorrectly been added to `hmpps-person-record`

You will know how to identify the records if you have got this far. It is likely that you have a CRN you wish to delete

You can run the SQL below to list the affected records:

```sql
select p.crn as source_system_id, p.source_system
from personrecordservice.person p
where p.crn in ('CRN1')
```

Which will return data in this format:

| source_system | source_system_id |
|---------------|------------------|
| DELIUS        | CRN1             |
| ...           | ...              |

First make sure that it returns the number of records which you expect, and that you have not mistyped a CRN

Then you need to export the data as a json into the format:

```json
[
  {
    "source_system" : "DELIUS",
    "source_system_id" : "CRN1"
  }
]
```

This can be saved into a file e.g. `records-to-delete.json`

This can be done in the DBeaver data exporter [guide](https://dbeaver.com/docs/dbeaver/Data-export/).

## 2. Delete records

To do this you must first port-forward to a `hmpps-person-record` pod.

Once a connection to the `hmpps-person-record` pod has been established you can then trigger the deletion.

This command forwards port 9090 to the application.

```shell
kubectl -n <namespace> port-forward deployment/hmpps-person-record 9090:8080
```

To test the connection to the pod, run the command:

```shell
curl -i http://localhost:9090/health
```

This should return an HTTP 200.

To delete the records, run this

```shell
curl -X POST http://localhost:9090/admin/delete \
     -H "Content-Type: application/json" \
     -d @</path/to/your/file/records-to-delete.json>
```

Once triggered, monitor the processing of the deletion in the logs. To see logs follow the [guide](002-Accessing-The-Logs.md). In particular, watch out for any CRNs which have not been deleted and find out why


## 3. Check deletion worked as expected

Re-run the query you used to generate of records-to-delete.json and check no records are returned

Check that the logs of the pod do not contain `Could not find person to delete`

Check tha event log table

`select * from personrecordservice.evemt_log where event_type = 'CPR_RECORD_DELETED' and source_system_id in ('CRN1')`




