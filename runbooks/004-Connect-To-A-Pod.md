# 004 - Connect to a Pod

This runbook is to describe the process of connecting and opening a shell connection to a pod running in kubernetes.

## Prerequisites

* Must have kubernetes access to the desired namespace
* Must have `kubectl`, which can be installed [here](https://kubernetes.io/docs/tasks/tools/#kubectl)

## Namespace

For hmpps-person-record the namespaces are listed below:
* `hmpps-person-record-dev`
* `hmpps-person-record-preprod`
* `hmpps-person-record-prod`


## Connect to the pod

To connect to the desired pod, run the following command:


```shell
kubectl exec -it deployment/hmpps-person-record -n <namespace> -- bash
```
