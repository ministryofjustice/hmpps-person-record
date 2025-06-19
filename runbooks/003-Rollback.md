# 003 - Rollback

This runbook outlines the steps to roll back model or person-matching changes that have resulted in incorrect or unstable clusters.

It reverts the system to the last known good version of the matching model and reprocesses the affected clusters and person records to restore consistency.

This rollback may be required due to:

* An invalid business logic change
* A bug in data processing 
* Or any issue that has caused data loss or incorrect clustering

The goal is to ensure that all clusters are returned to a valid and reliable state.

## Table of contents

1. [Dependencies](#dependencies)
2. [Identify the affected version](#1-identify-the-affected-version)
3. [Rollback Changes](#2-rollback-changes)
4. [Identify affected clusters and records](#3-identify-affected-clusters-and-records)
5. [Reprocess affected clusters](#4-reprocess-affected-clusters)
6. [Assess Business Impact](#5-assess-business-impact)
7. [Resolve Outstanding Need Attention Clusters](#5-assess-business-impact)
8. [Optional: Cluster sanity checking](#7-optional-cluster-sanity-checking)

## Dependencies

* You need a database connection to the read-replica database of `hmpps-person-record`. To do this see:
  * [Connecting to the database](001-Connecting-To-The-Database.md)

* This service is tightly coupled to `hmpps-person-match` which is responsible for matching:
  * [hmpps-person-match](https://github.com/ministryofjustice/hmpps-person-match)

## 1. Identify the affected version

Firstly, identify the deployment of  `hmpps-person-match` or `hmpps-person-record` that has caused the unwanted behaviour. 

Once identified, then locate the deployment time that change has been running from. You can find this by looking in CircleCI or Github at the deployment. This will be in the format:
```
yyyy-MM-dd HH:mm:ss
```

We can use this to identify any clusters and records that have been updated and processed with the unwanted changes.

## 2. Rollback Changes

Now you can roll back the system to the state before the unwanted changes took effect.

To do this, raise a Git pull request in the affected repository containing the rollback changes.
Ensure the pull request is reviewed and approved by a team member before merging.

Once merged, release the rollback as appropriate and verify that the system is functioning as expected.

## 3. Identify affected records

Once the rollback is in place and processing messages as normal, the next step is to identify the 
records that have been affected due to the unwanted change. 

To do this, the timestamp of the deployment identified in [step one](#1-identify-the-affected-version) can be 
used to interrogate the event log to identify affected records.

To can this list run the following SQL with the identified SQL:

```sql
select el.source_system_id, el.source_system
from personrecordservice.event_log el
where el.event_type in ('CPR_RECORD_CREATED', 'CPR_RECORD_UPDATED') and el.event_timestamp >= '<timestamp>'
group by el.source_system_id, el.source_system 
```

Which will return a table as such:

| source_system   | source_system_id                     |
|-----------------|--------------------------------------|
| COMMON_PLATFORM | 6d6fa987-c680-4f18-ba58-ec507463a75c |
| ...             | ...                                  |

Then you need to export the data as a json into the format:

```json
[
  {
    "source_system" : "COMMON_PLATFORM",
    "source_system_id" : "6d6fa987-c680-4f18-ba58-ec507463a75c"
  }
]
```

This can be saved into a file e.g. `affected-records.json`

This can be done in the DBeaver data exporter [guide](https://dbeaver.com/docs/dbeaver/Data-export/).

## 4. Re-process affected records

This can now be sent to the admin endpoint to reprocess and recluster the affected records.

To do this you must first port-forward to a `hmpps-person-record` pod.

Once a connection to the `hmpps-person-record` pod has been established you can then trigger the reprocessing of the affected clusters.

This command forwards port 9090 to the application.

```shell
kubectl -n <namespace> port-forward deployment/hmpps-person-record 9090:8080
```

To test the connection to the pod, run the command:

```shell
curl -i http://localhost:9090/health
```

This should return an HTTP 200.

This process will take the cluster that the record is currently in and determine whether it is still valid or it needs looking at.
Using the saved file `affected-records.json` from [step three](#3-identify-affected-records), trigger the recluster endpoint by running the command:

```shell
curl -X POST http://localhost:9090/admin/recluster \
     -H "Content-Type: application/json" \
     -d @</path/to/your/file/affected-recluster.json>
```

Once triggered, monitor the processing of the cluster in the logs. To see logs follow the [guide](002-Accessing-The-Logs.md).

During testing, it took approximately three minutes to process 500 records in the development environment.

## 5. Assess Business Impact

Now that the changes have been rolled back and the affected records been reprocessed.

This is a good time to analyze the impact the unwanted change has had on the business.
If any communication with the wider organisation needs to happen, communicate this out.

## 6. Resolve Outstanding Needs Attention Clusters

Even after reprocessing, there can be still clusters left in an `NEEDS_ATTENTION` state which need a manual intervention to resolve.

This needs to be evaluated and prioritised to resolve these erroneous clusters / records. 
Following the defined manual process of resolving the clusters, TODO.

## 7. Optional: Cluster sanity checking

To validate the accuracy of the reprocessed clusters, we can optionally ask the data science team to perform a sanity check to ensure that no clusters were mistakenly merged.

This is to prevent any false positives being introduced into the system.

