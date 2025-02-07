# 001 - Seeding Person Match

This is the runbook to send person match all the person data that it needs to use to match with.

## Prerequisites

* Must have kubernetes access to the desired namespace
* Must have `kubectl`, which can be installed [here](https://kubernetes.io/docs/tasks/tools/#kubectl)

## Namespace

For hmpps-person-record the namespaces are listed below:
* `hmpps-person-record-dev`
* `hmpps-person-record-preprod`
* `hmpps-person-record-prod`

## 1. Start Seeding Person Match

Once the message consumption has stopped, you can start the seeding process.

To kick off the process you must connect to the hmpps-person-record pod first, by:

```shell
kubectl exec -it deployment/hmpps-person-record -n <namespace> -- bash
```

Then within the pod run, to kick off the desired process:

    > WARNING:
    > You must not deploy to the environment that scheduled for reseeding once the job has started. Otherwise, it will be cancelled.
    >
    > In the event this occurs, verify message processing is still paused and follow from step 2 again.

To trigger process:
```shell
curl -i -X POST http://localhost:8080/populatepersonmatch
```
The process will output the number of pages and records to be processed.
It will notify once finished with: `Finished populating person-match, total pages: <totalPages>, total elements: <totalElements>"`

## Troubleshooting

### Seeding Processing Fails

If the processing of messages fails to create the new records. Either from a mapping issue or api issue. Prepare a fix then follow from step 2 to proceed with seeding the data.