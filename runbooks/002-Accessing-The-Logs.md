# 002 - How to Access the Logs

This runbook describes how to view and access the application logs.

## Table of contents

1. [Through Kubernetes](#through-kubernetes)
2. [Through AppInsights](#through-appinsights)

## Through Kubernetes

You can read the logs using [kubectl](https://kubernetes.io/docs/reference/kubectl/).
To view logs from the currently deployed application:

```shell
kubectl logs deployment/hmpps-person-record -n hmpps-person-record-<namespace>
```

## Through AppInsights

To view logs in AppInsights use these environment indexes.

**Environment**:
* [Development (nomisapi-t3)](https://portal.azure.com/#@nomsdigitechoutlook.onmicrosoft.com/resource/subscriptions/c27cfedb-f5e9-45e6-9642-0fad1a5c94e7/resourceGroups/nomisapi-t3-rg/providers/Microsoft.Insights/components/nomisapi-t3/logs)
* [Pre Production (nomisapi-preprod)](https://portal.azure.com/#@nomsdigitechoutlook.onmicrosoft.com/resource/subscriptions/a5ddf257-3b21-4ba9-a28c-ab30f751b383/resourceGroups/nomisapi-preprod-rg/providers/Microsoft.Insights/components/nomisapi-preprod/logs)
* [Production (nomisapi-prod)](https://portal.azure.com/#@nomsdigitechoutlook.onmicrosoft.com/resource/subscriptions/a5ddf257-3b21-4ba9-a28c-ab30f751b383/resourceGroups/nomisapi-prod-rg/providers/Microsoft.Insights/components/nomisapi-prod/logs)

You can view logs through Azure AppInsights. To filter this application use:

```
AppTraces
| where AppRoleName == 'hmpps-person-record'
```
