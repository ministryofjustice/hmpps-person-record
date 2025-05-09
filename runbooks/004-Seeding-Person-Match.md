# 004 - Seeding Person Match

This is the runbook to send person match all the person data that it needs to use to match with.

## Prerequisites

* Data from person match database must clean deleted
* Must have the `seeding` profile active
* Must have kubernetes access to the desired namespace
* Must have `kubectl`, which can be installed [here](https://kubernetes.io/docs/tasks/tools/#kubectl)

## Namespace

For hmpps-person-record the namespaces are listed below:
* `hmpps-person-record-dev`
* `hmpps-person-record-preprod`
* `hmpps-person-record-prod`

## 1. Deleting data from person match database
```
delete from personmatch.person
```

## 2. Start Seeding Person Match

To kick off the process you must connect to the hmpps-person-record pod first, by:

```shell
kubectl exec -it deployment/hmpps-person-record -n <namespace> -- bash
```

Then within the pod run, to kick off the desired process:

    > WARNING:
    > You must not deploy to the environment that scheduled for seeding once the job has started. Otherwise, it will be cancelled.

To trigger process:
```shell
curl -i -X POST http://localhost:8080/populatepersonmatch
```
Once the process has completed it will output the number of pages and records processed.
It will notify once finished with: `Finished populating person-match, total pages: <totalPages>, total elements: <totalElements>, time elapsed: <time_elapsed>"`

## Troubleshooting

### Seeding Processing Fails

If the processing of messages fails to create the new records. Either from a mapping issue or api issue. Prepare a fix then follow from step 2 to proceed with seeding the data.