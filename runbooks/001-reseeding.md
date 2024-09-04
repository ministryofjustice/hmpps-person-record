# 001 - Reseeding

This is the runbook to delete all current data from the database and repopulate the information from NOMIS and NDELIUS.
This includes switching off message processing and a UUID linking process.

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

Then to delete, run these SQL scripts to delete the desired data for the desired source system:

For **NOMIS**:
```
delete from personrecordservice.person p where p.source_system = 'NOMIS'
```

For **DELIUS**:
```
delete from personrecordservice.person p where p.source_system = 'DELIUS'
```

Verify data has been deleted.

# 3. Start Reseeding Jobs

Once the data has been cleared and message consumption has stopped, you can start the reseeding jobs.

To kick off the jobs you must connect to the hmpps-person-record pod first, by:

```shell
kubectl exec -it deployment/hmpps-person-record -n <namespace> -- bash
```


Then within the pod run, to kick off the desired job:

> WARNING:
> You must not deploy to the environment that scheduled for reseeding once the job has started. Otherwise, it will be cancelled.
>
> In the event this occurs, verify message processing is still paused and follow from step 2 again.

For **NOMIS** (~90mins to 3h):
```
curl -i -X POST http://localhost:8080/populatefromprison 
```
Will notify once finished with: `Prison seeding finished, approx records <number>`

For **DELIUS** (~7-8 hours):
```
curl -i -X POST http://localhost:8080/populatefromprobation
```
Will notify once finished with: `DELIUS seeding finished, approx records <number>`

# 4. Record Linking

To link the seeded data from a provided data cluster.

Follow: [Link Cluster Data](./002-link-cluster-data.md)

# 5. Resume Message Consumption
 
Resume message consumption by removing the profile `seeding` to the spring configuration in helm.
This is done by removing it from the desired environment helm chart spring environment variable, which can be found at `helm_deploy/hmpps-person-record/values-<environment>.yaml`:

```
env:
    SPRING_PROFILES_ACTIVE: "<environment>"
```

Where `<environment>` is the values of: `dev`, `preprod` or `prod`.

Which the code change then needs a PR raising and releasing to the desired environment.

Verify message consumption has resumed.
