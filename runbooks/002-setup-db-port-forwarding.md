# 002 - Setup Database Port Forwarding

This runbook is a guide to establish a connection to desired environments database instance.

## Prerequisites

* Must have kubernetes access to the desired namespace
* Must have `kubectl`, which can be installed [here](https://kubernetes.io/docs/tasks/tools/#kubectl)
* Must have `cloud-platform` cli, which can be installed [here](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/getting-started/cloud-platform-cli.html#cloud-platform-cli)

# 1. Retrieve database credentials

To connect to the database instance you need to first retrieve the credentials i.e. database name, username and password.

These are stored as kubernetes secrets, which can be found by running:

```shell
kubectl get secrets -n <namespace>
```

This should contain a secret named `<namespace>-rds-instance-output` for this project: `hmpps-person-record-rds-instance-output`

The secrets are encoded, so you will need to use the cloud-platform cli tool to decode the rds secret.

To do this run:

```shell
cloud-platform decode-secret -s hmpps-person-record-rds-instance-output -n <namespace>
```

Which will provide you with:
* `database_name`
* `database_password`
* `database_username`
* `rds_instance_address`

# 2. Create port forward service pod

To be able to connect to the database instance you must first port forward, first create a port forward pod to pass our connection through.

> WARNING:
> Make sure any local running database instances are using a different port. As it may cause unwanted actions on the instance database.

To create the service pod run:

```shell
kubectl -n <namespace> run port-forward-pod 
    --image=ministryofjustice/port-forward 
    --port=5432 --env="REMOTE_HOST=<rds_instance_address>” 
    --env="LOCAL_PORT=5432" 
    --env="REMOTE_PORT=5432"
```

Such that the values are:
* `<namespace>` = kubernetes namespaces e.g. `hmpps-person-record-preprod`
* `<rds_instance_address>` = the rds_instance_address retrieved in step 1.

# 3. Start the port forward pod

Then you will be able to start the port forward pod to allow connections.

To do this run:

```shell
kubectl port-forward port-forward-pod -n <namespace> 5432:5432 &
```

# 4. Stop port forwarding

Once all required actions have been conducted then ensure you have stop port forwarding by terminating the connection to the pod.

Then delete the service pod:

```
kubectl delete pod port-forward-pod -n <namespace>
```
