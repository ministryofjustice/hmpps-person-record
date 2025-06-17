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
2. [Identify the effected version](#1-identify-the-effected-version)
3. [Rollback Changes](#2-rollback-changes)
4. [Identify effected clusters and records](#3-identify-effected-clusters-and-records)
5. [Reprocess effected clusters](#4-reprocess-effected-clusters)

## Dependencies

* You need a database connection to the read-replica database of `hmpps-person-record`. To do this see:
  * [Connecting to the database](001-Connecting-To-The-Database.md)

* This service is tightly coupled `hmpps-person-match` as it is responsible for matching:
  * [hmpps-person-match](https://github.com/ministryofjustice/hmpps-person-match)

## 1. Identify the effected version

Firstly, identify the deployment that has caused the unwanted behaviour, regardless whether it has 
originated from `hmpps-person-match` or `hmpps-person-record`. 

Once identified, then locate the deployment time that change has been running from. This will be in the format:
```
yyyy-MM-dd HH:mm:ss
```

We can use this to identify any clusters + records that have been updated and processed with the unwanted changes.

## 2. Rollback Changes

Now you can roll back the system to the state before the unwanted changes took effect.

To do this, raise a Git pull request in the affected repository containing the rollback changes.
Ensure the pull request is reviewed and approved by a team member before merging.

Once merged, release the rollback as appropriate and verify that the system is functioning as expected.

## 3. Identify effected clusters and records

Once, the rollback is in place and processing messages as normal. We can now being to identify the 
clusters that have been effected due to the unwanted change. 

To do this, the timestamp of the deployment identified in [step one](#1-identify-the-effected-version) can be 
used to interrogate the event log to identify effected clusters and records.

To can this list run the following SQL with the identified SQL:

```sql
select el.person_uuid, el.source_system, el.source_system_id
from personrecordservice.event_log el
where el.event_type in ('CPR_RECORD_CREATED', 'CPR_RECORD_UPDATED') and el.event_timestamp >= '<timestamp>'
```

Which will return a table as such:

| person_uuid                          | source_system   | source_system_id                     |
|--------------------------------------|-----------------|--------------------------------------|
| 782a5cbe-5a10-4694-a046-0be9e82f0591 | COMMON_PLATFORM | 6d6fa987-c680-4f18-ba58-ec507463a75c |
| ...                                  | ...             | ...                                  |

Then you need to export the data as a json into the format:

```json
[
  {
    "person_uuid" : "782a5cbe-5a10-4694-a046-0be9e82f0591",
    "source_system" : "COMMON_PLATFORM",
    "source_system_id" : "6d6fa987-c680-4f18-ba58-ec507463a75c"
  }
]
```

This can be saved into a file e.g. `effected-clusters.json`

This can be done in the DBeaver data exporter [guide](https://dbeaver.com/docs/dbeaver/Data-export/).

## 4. Re-process effected clusters

This can now be sent to the admin endpoint to reprocess and recluster the effected clusters.

To do this you must first port-forward to `hmpps-person-record` pod.

Once a connection to the `hmpps-person-record` pod has been established you can then trigger the reprocessing of the effected clusters.

Run the command, to find the pod name of a `hmpps-person-record` pod.

```shell
kubectl get pods -n <namespace> --field-selector=status.phase=Running | grep 'hmpps-person-record'
```

Then port forward to that pod, to allow for us to call the admin endpoint with our json file. Using the command:

```shell
kubectl -n <namespace> port-forward <pod-name> 8080:9090
```

Which opens port on 9090 to the application.

Using the saved file `effected-clusters.json` from [step three](#3-identify-effected-clusters-and-records), trigger the recluster endpoint, run the command:

```shell
curl -X POST http://localhost:9090/admin/recluster \
     -H "Content-Type: application/json" \
     -F "file=@</path/to/your/file/effected-recluster.json>"
```

Once triggered, monitor the processing of the cluster in the logs. To see logs follow the [guide](002-Accessing-The-Logs.md).

## 5. Assess Business Impact

Now that the changes have been rolled back and the effected clusters been reprocessed.

This is a good time to analyze the impact the unwanted change has had on the business.
If any communication with the wider organisation needs to happen, communicate this out.

## 6. Resolve Outstanding Need Attention Clusters

Even after reprocessing, there can be still clusters left in an `NEEDS_ATTENTION` state which needs a manual intervention to resolve.

This needs to be evaluated and prioritised to resolve these erroneous clusters / records. 
Following the defined manual process of resolving the clusters, TODO.

## 7. Optional: Cluster sanity checking

To validate the accuracy of the reprocessed clusters, we can optionally ask the data science team to perform a sanity check to ensure that no clusters were mistakenly merged.

This is to prevent any false positives being introduced into the system.

