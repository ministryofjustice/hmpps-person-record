# 002 - Setup Database Port Forwarding

This runbook is a guide to establish a connection to desired environments database instance.

## Prerequisites

* Must have kubernetes access to the desired namespace
* Must have `kubectl`, which can be installed [here](https://kubernetes.io/docs/tasks/tools/#kubectl)
* Must have `cloud-platform` cli, which can be installed [here](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/getting-started/cloud-platform-cli.html#cloud-platform-cli)

# 1. Create port forward service pod

To be able to port forward to the database instance, create a port forward pod to pass our connection through.

To create the service pod run:

```
kubectl -n <namespace> run port-forward-pod 
    --image=ministryofjustice/port-forward 
    --port=5432 --env="REMOTE_HOST=<rds_instance_address>” 
    --env="LOCAL_PORT=5432" 
    --env="REMOTE_PORT=5432"
```

Such that the values are:
* `<namespace>` = kubernetes namespaces e.g. `hmpps-person-record-preprod`
* `<rds_instance_address>` = the rds instance address, retrieved in step 1.
