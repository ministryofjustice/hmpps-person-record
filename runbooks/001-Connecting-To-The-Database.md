# 001 - Connecting to the database

This is a runbook on how to connect and start querying the `hmpps-person-record` or `hmpps-person-match` database.

Following the process from the cloud-platform [guide](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/other-topics/rds-external-access.html#accessing-your-rds-database).

## Table of contents

1. [Prerequisites](#prerequisites)
2. [Namespace](#namespace)
3. [Establishing the connection](#establishing-the-connection)
   1. [Creating the Port Forward Pod](#creating-the-port-forward-pod)
   2. [Port forward to the database](#port-forward-to-the-database)
4. [Connecting to the database](#connecting-to-the-database)

## Prerequisites

* Must have kubernetes access to the desired namespace
* Must have `kubectl`, which can be installed [here](https://kubernetes.io/docs/tasks/tools/#kubectl)

## Namespace

For hmpps-person-record the namespaces are listed below:
* `hmpps-person-record-dev`
* `hmpps-person-record-preprod`
* `hmpps-person-record-prod`

## Establishing the connection

To be able to connect to the database you need to port forward to the desired database. 
To do this there must be a kubernetes port-forward pod available to establish the connection.

To check if a port forward pod exists, which will be named either:
* `person-match-port-forward-pod`
* `person-record-port-forward-pod`

Run the following command:

```shell
kubectl get pods -n <namespace> | grep port-forward
```

If the required pod does not exist, follow the steps defined in [Creating the Port Forward Pod](#creating-the-port-forward-pod).
If it exists, follow the steps in [Port forward to the database](#port-forward-to-the-database)

### Creating the Port Forward Pod

To create the port forward pod if it does not exist, you must first gather the database credentials.
Which are located in the kubernetes secrets.

To get the secrets run:

```shell
kubectl get secrets -n <namespace>
```

Then for `hmpps-person-record` database credentials, run the command:

> **_NOTE:_** This is a read replica, meaning you can only do read-only operations with this database connection which is suitable for almost all purposes.

```shell
cloud-platform decode-secret -s hmpps-person-record-rds-read-replica-instance-output -n <namespace> | jq -r '.data.rds_instance_address'
```

For `hmpps-person-match` database credentials, run the command:

```shell
cloud-platform decode-secret -s hmpps-person-match-rds-instance-output -n <namespace> | jq -r '.data.rds_instance_address'
```

Which gets the `rds_instance_address` value which says which database to port forward to.

Then to create the port forward pod using the credentials from the steps above, run the command:

```shell
kubectl \
  -n <namespace> \
  run port-forward-pod \
  --image=ministryofjustice/port-forward \
  --port=5432 \
  --env="REMOTE_HOST=<rds_instance_address>" \
  --env="LOCAL_PORT=5432" \
  --env="REMOTE_PORT=5432"
```

### Port forward to the database

Once the port forward pod exists you can then use it to establish the connection to the database.

To start port forwarding to the database, run the command:

```shell
kubectl \
  -n <namespace> \
  port-forward \
  port-forward-pod 5432:5432
```

## Connecting to the database

Once the connection is established by port forwarding, you can then connect to the database using the credentials used
in the [Creating a port forward pod](#creating-the-port-forward-pod) steps.

To get the database name, password and user from the secrets.
from the values:

* NAME: "database_name"
* USERNAME: "database_username"
* PASSWORD: "database_password"
