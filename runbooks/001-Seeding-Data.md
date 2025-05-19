# 001 - Seeding Data

This is the runbook to delete all current data from the database and repopulate the information from NOMIS and NDELIUS.
This includes switching off message processing and a UUID linking process.

As an alternative to this, you can choose to [update all existing](#update) Delius data without switching off message processing.  

## Prerequisites

* Must have kubernetes access to the desired namespace
* Must have `kubectl`, which can be installed [here](https://kubernetes.io/docs/tasks/tools/#kubectl)

## Namespace

For hmpps-person-record the namespaces are listed below:
* `hmpps-person-record-dev`
* `hmpps-person-record-preprod`
* `hmpps-person-record-prod`

## 1. Pause Message Consumption

Firstly, pause message consumption by adding the profile `seeding` to the spring configuration in helm.
This is done by adding it to the desired environment helm chart spring environment variable, which can be found at `helm_deploy/hmpps-person-record/values-<environment>.yaml`:

```
env:
    SPRING_PROFILES_ACTIVE: "<environment>, seeding"
```

Where `<environment>` is the values of: `dev`, `preprod` or `prod`.

Which the code change then needs a PR raising and releasing to the desired environment.

## 2. Delete Existing Data

Secondly, delete the already existing data (if any) from the database for specific source systems.

To connect to the desired database follow these step to connect to the database [using the process defined](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/other-topics/rds-external-access.html)

Then to delete all records in the database in the order outlined below:

For **person table**:
```
delete from personrecordservice.person
```

For **person_key table**:
```
delete from personrecordservice.person_key
```

Finally **event_log table**:
```
delete from personrecordservice.event_log
```


Verify data has been deleted.

```
select count(*) from personrecordservice.person
select count(*) from personrecordservice.person_key
select count(*) from personrecordservice.event_log

```

## 3. Start Reseeding

Once the data has been cleared and message consumption has stopped, you can start the reseeding processes.

To kick off the processes you must connect to the hmpps-person-record pod first, by:

```shell
kubectl exec -it deployment/hmpps-person-record -n <namespace> -- bash
```


Then within the pod run, to kick off the desired process:

    > WARNING:
    > You must not deploy to the environment that scheduled for reseeding once the job has started. Otherwise, it will be cancelled.
    >
    > In the event this occurs, verify message processing is still paused and follow from step 2 again.

For **NOMIS** (~90 minutes to 3 hours):
```shell
curl -i -X POST http://localhost:8080/populatefromprison 
```
The process will output the number of pages and records to be processed.
It will notify once finished with: `Prison seeding finished, approx records <number>`

For **DELIUS** (~7-8 hours):
```shell
curl -i -X POST http://localhost:8080/populatefromprobation
```

The process will output the number of pages and records to be processed.
It will notify once finished with: `DELIUS seeding finished, approx records <number>`

## 4. Seeding person match

Follow: [Seed hmpps-person-match](./004-Seeding-Person-Match.md)

Generate clusters (~2 hours)
- inform Robin once hmpps-person-match seeding is done
- he will verify all records are present
- he will run a script to generate UUIDs for every record - output to be a UUID and the MATCH_ID of each record
- data will now be on Robin's laptop
- Robin will transfer data to hmpps-person-record database in a temporary table,
  - use postgres import to create a temporary table in hmpps-person-record from the csv


Fallback idea  
- onedrive will be used to transfer a csv with ~4 million columns with UUID and the MATCH_ID

Put clusters into hmpps-person-record (~30 minutes)
TEST PROCESS IN DEV FOR APPROXIMATE TIMINGS

## 5. Linking Records To Clusters
See [002-Link-Cluster-Data.md](002-Link-Cluster-Data.md)

## 6. Resume Message Consumption
 
Resume message consumption by removing the profile `seeding` to the spring configuration in helm.
This is done by removing it from the desired environment helm chart spring environment variable, which can be found at `helm_deploy/hmpps-person-record/values-<environment>.yaml`:

```
env:
    SPRING_PROFILES_ACTIVE: "<environment>"
```

Where `<environment>` is the values of: `dev`, `preprod` or `prod`.

Which the code change then needs a PR raising and releasing to the desired environment.

Verify message consumption has resumed.

## Troubleshooting

### Seeding Processing Fails

If the processing of messages fails to create the new records. Either from a mapping issue or api issue. Prepare a fix then follow from step 2 to proceed with seeding the data.

# Update

It is possible to update all Delius records rather than deleting and recreating them. This is considerably slower than deleting and recreating but it keeps the existing clusters in place. Last time it ran it took approximately 2 days. Frequent interventions were required to restart the job from the point at which it failed.

To kick off the process you must connect to the hmpps-person-record pod first, by:

```shell
kubectl exec -it deployment/hmpps-person-record -n <namespace> -- bash
```

For **DELIUS** (~48 hours):
```shell
curl -i -X POST http://localhost:8080/updatefromprobation?startPage=1
```

The process will notify once finished with: `DELIUS updates finished, approx records <number>`

In the event that it fails, note the last page which was processed. Each page is logged in this format:
`Processing DELIUS updates, page: 2163 / 2280 | trace_id=1cfd02433c3ebd5bfb6756d0680118c2, trace_flags=01, span_id=06711d0445c0aa40 `

Then connect to the hmpps-person-record pod:

```shell
kubectl exec -it deployment/hmpps-person-record -n <namespace> -- bash
```

And restart the job from the same page on which it failed

```shell
curl -i -X POST http://localhost:8080/updatefromprobation?startPage=2163
```

> WARNING:
> You must not deploy to the environment that scheduled for reseeding once the process has started. Otherwise, it will be cancelled and you will have to restart from the page on which it was cancelled.
>

