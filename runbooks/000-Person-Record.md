# HMPPS Person Record Runbook

This is a runbook to document how this service is supported, as described in: [MOJ Runbooks](https://technical-guidance.service.justice.gov.uk/documentation/standards/documenting-how-your-service-is-supported.html#what-you-should-include-in-your-service-39-s-runbook)

## Table of Contents
1. [Introduction](#Introduction)
   1. [Dependent Services](#Dependent-Services)
2. [Service URLs](#Service-URLs)
3. [Incident Response Hours](#Incident-Response-Hours)
4. [Incident Contact Details](#Incident-Contact-Details)
5. [Service Team Contact](#service-team-contact)
6. [Expected Speed and Frequency of Releases](#expected-speed-and-frequency-of-releases)
7. [Automatic alerts](#automatic-alerts)
8. [Impact of an outage](#impact-of-an-outage)
9. [Monitoring](#monitoring)
   1. [Development (Dev)](#development-dev)
   2. [Pre-Production (PreProd)](#pre-production-preprod)
   3. [Production (Prod)](#production-prod)
10. [Logs](#logs)
    1. [Through Kubernetes](#through-kubernetes)
    2. [Through AppInsights](#through-appinsights)
11. [How to resolve specific issues](#how-to-resolve-specific-issues)

## Introduction

A service for managing identity data about the people we look after in HMPPS.

This Kotlin application integrates with a PostgreSQL database to manage and store person-related information.
It consumes messages from AWS SQS queues and republishes them to a designated topic. Additionally, it exposes a RESTful API for accessing and managing person records, built using Spring Boot.

### Dependent Services

* [hmpps-person-match](https://github.com/ministryofjustice/hmpps-person-match): This is a Python application built with FastAPI that accepts and stores person information for the purpose of matching and scoring records against one another.
    It integrates with a PostgreSQL database to manage and retrieve relevant data.
* [cloud-platform-environments](https://github.com/ministryofjustice/cloud-platform-environments): This is where we define and manage the infrastructure for this service. Maintained by the cloud-platform team. We use prebuilt terraform templates to define and provision AWS / Kubernetes resources.

## Service URLs

- Development: https://hmpps-person-record-dev.hmpps.service.justice.gov.uk
- Pre-Production: https://hmpps-person-record-preprod.hmpps.service.justice.gov.uk
- Production: https://hmpps-person-record.hmpps.service.justice.gov.uk

## Incident Response Hours

Office hours, usually 9am-5pm on working days.

## Incident Contact Details

Email: [hmpps-person-record@digital.justice.gov.uk](mailto:hmpps-person-record@digital.justice.gov.uk)

## Service Team Contact

The service team can be reached on MOJ Slack: [#hmpps-person-record](https://moj.enterprise.slack.com/archives/C04AQPM3A73)

## Expected Speed and Frequency of Releases

Trunk based development and continuous integration is practiced on this service. If changes pass all automated tests, they are deployed all the way to production.
There is no change request process and the delivery team pushes to production regularly (multiple times a day on average).

## Automatic alerts

These include:
- Security scan results (Trivy, OWASP, Veracode)
- Healthcheck Alerts
- SQS DLQ Messages Alerts
- Deployments

## Impact of an outage

Since we have one consumer (in a beta phase), the impact across the organisation is minimal.

## Monitoring

See environment specific monitoring links:

### Development (Dev)

* **Application**:
  * [Memory and CPU for hmpps-person-record](https://grafana.live.cloud-platform.service.justice.gov.uk/d/a164a7f0339f99e89cea5cb47e9be617/kubernetes-compute-resources-workload?orgId=1&refresh=10s&from=now-1h&to=now&var-datasource=default&var-cluster=&var-namespace=hmpps-person-record-dev&var-workload=hmpps-person-record&var-type=deployment)
  * [Memory and CPU for hmpps-person-record RDS](https://grafana.live.cloud-platform.service.justice.gov.uk/d/VR46pmwWk/aws-rds?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-dbinstanceidentifier=cloud-platform-21758fcf16e3a488&from=now-6h&to=now)
  * [Memory and CPU for hmpps-person-match](https://grafana.live.cloud-platform.service.justice.gov.uk/d/a164a7f0339f99e89cea5cb47e9be617/kubernetes-compute-resources-workload?orgId=1&refresh=10s&from=now-1h&to=now&var-datasource=prometheus&var-cluster=&var-namespace=hmpps-person-record-dev&var-workload=hmpps-person-match&var-type=deployment&timezone=utc)
  * [Memory and CPU for hmpps-person-match RDS](https://grafana.live.cloud-platform.service.justice.gov.uk/d/VR46pmwWk/aws-rds?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-dbinstanceidentifier=cloud-platform-630cc18efc9725ba&from=now-3h&to=now&timezone=browser)
* **AWS**:
  * **Queues**:
    * **Courts**:
      * [CPR Court Case Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-development-cpr_court_cases_queue.fifo&from=now-24h&to=now&timezone=browser)
      * [CPR Court Case Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-development-cpr_court_cases_dlq.fifo&from=now-24h&to=now&timezone=browser)
    * **Delius**:
      * [Delius Offender Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=Cloudwatch&var-region=default&var-queue=hmpps-person-record-development-cpr_delius_offender_events_queue)
      * [Delius Offender Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=Cloudwatch&var-region=default&var-queue=hmpps-person-record-development-cpr_delius_offender_events_dlq)
      * [Delius Merge Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-development-cpr_delius_merge_events_queue&from=now-24h&to=now)
      * [Delius Merge Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-development-cpr_delius_merge_events_dlq&from=now-24h&to=now)
      * [Delius Delete Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-development-cpr_delius_delete_events_queue&from=now-24h&to=now)
      * [Delius Delete Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-development-cpr_delius_delete_events_dlq&from=now-24h&to=now)
    * **Nomis**:
      * [Nomis Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-development-cpr_nomis_events_queue&from=now-24h&to=now)
      * [Nomis Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-development-cpr_nomis_events_dlq&from=now-24h&to=now)
      * [Nomis Merge Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-development-cpr_nomis_merge_events_queue&from=1723710226408&to=1723796626408)
      * [Nomis Merge Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-development-cpr_nomis_merge_events_dlq&from=1723710286631&to=1723796686632)
  * **Topics**:
    * [CPR Court Case Topic](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSNS001/aws-sns?from=now-12h&to=now&timezone=browser&var-datasource=P896B4444D3F0DAB8&var-region=default&var-topic=cloud-platform-hmpps-person-record-6db24f48dcd80b9e5d946f554d5e931f)

### Pre-Production (Preprod)

* **Application**:
    * [Memory and CPU for hmpps-person-record](https://grafana.live.cloud-platform.service.justice.gov.uk/d/a164a7f0339f99e89cea5cb47e9be617/kubernetes-compute-resources-workload?orgId=1&refresh=10s&from=now-1h&to=now&var-datasource=default&var-cluster=&var-namespace=hmpps-person-record-preprod&var-workload=hmpps-person-record&var-type=deployment)
    * [Memory and CPU for hmpps-person-record RDS](https://grafana.live.cloud-platform.service.justice.gov.uk/d/VR46pmwWk/aws-rds?orgId=1&var-datasource=Cloudwatch&var-region=default&var-dbinstanceidentifier=cloud-platform-288cab966b34da54&from=now-6h&to=now)
    * [Memory and CPU for hmpps-person-match](https://grafana.live.cloud-platform.service.justice.gov.uk/d/a164a7f0339f99e89cea5cb47e9be617/kubernetes-compute-resources-workload?orgId=1&refresh=10s&from=now-6h&to=now&var-datasource=prometheus&var-cluster=&var-namespace=hmpps-person-record-preprod&var-workload=hmpps-person-match&var-type=deployment&timezone=utc)
    * [Memory and CPU for hmpps-person-match RDS](https://grafana.live.cloud-platform.service.justice.gov.uk/d/VR46pmwWk/aws-rds?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-dbinstanceidentifier=cloud-platform-05509d3640870a0b&from=now-6h&to=now&timezone=browser)
* **AWS**:
    * **Queues**:
        * **Courts**:
            * [CPR Court Case Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-preprod-cpr_court_cases_queue.fifo&from=now-24h&to=now&timezone=browser)
            * [CPR Court Case Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-preprod-cpr_court_cases_dlq.fifo&from=now-24h&to=now&timezone=browser)
        * **Delius**:
            * [Delius Offender Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=Cloudwatch&var-region=default&var-queue=hmpps-person-record-preprod-cpr_delius_offender_events_queue)
            * [Delius Offender Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=Cloudwatch&var-region=default&var-queue=hmpps-person-record-preprod-cpr_delius_offender_events_dlq)
            * [Delius Merge Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-preprod-cpr_delius_merge_events_queue&from=now-24h&to=now)
            * [Delius Merge Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-preprod-cpr_delius_merge_events_dlq&from=now-24h&to=now)
            * [Delius Delete Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-preprod-cpr_delius_delete_events_queue&from=now-24h&to=now)
            * [Delius Delete Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-preprod-cpr_delius_delete_events_dlq&from=now-24h&to=now)
        * **Nomis**:
            * [Nomis Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-preprod-cpr_nomis_events_queue&from=now-24h&to=now)
            * [Nomis Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-preprod-cpr_nomis_events_dlq&from=now-24h&to=now)
            * [Nomis Merge Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-preprod-cpr_nomis_merge_events_queue&from=now-24h&to=now&timezone=browser)
            * [Nomis Merge Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-preprod-cpr_nomis_merge_events_dlq&from=now-24h&to=now&timezone=browser)
    * **Topics**:
      * [CPR Court Case Topic](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSNS001/aws-sns?from=now-12h&to=now&timezone=browser&var-datasource=P896B4444D3F0DAB8&var-region=default&var-topic=cloud-platform-hmpps-person-record-aacba4b10ce095157765bb00c8bf5d36)

### Production (Prod)

* **Application**:
    * [Memory and CPU for hmpps-person-record](https://grafana.live.cloud-platform.service.justice.gov.uk/d/a164a7f0339f99e89cea5cb47e9be617/kubernetes-compute-resources-workload?orgId=1&refresh=10s&from=now-1h&to=now&var-datasource=default&var-cluster=&var-namespace=hmpps-person-record-prod&var-workload=hmpps-person-record&var-type=deployment)
    * [Memory and CPU for hmpps-person-record RDS](https://grafana.live.cloud-platform.service.justice.gov.uk/d/VR46pmwWk/aws-rds?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-dbinstanceidentifier=cloud-platform-325c1d58e0fe99fe&from=now-6h&to=now)
    * [Memory and CPU for hmpps-person-match](https://grafana.live.cloud-platform.service.justice.gov.uk/d/a164a7f0339f99e89cea5cb47e9be617/kubernetes-compute-resources-workload?orgId=1&refresh=10s&from=now-6h&to=now&var-datasource=prometheus&var-cluster=&var-namespace=hmpps-person-record-prod&var-workload=hmpps-person-match&var-type=deployment&timezone=utc)
    * [Memory and CPU for hmpps-person-match RDS](https://grafana.live.cloud-platform.service.justice.gov.uk/d/VR46pmwWk/aws-rds?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-dbinstanceidentifier=cloud-platform-ee08837ab8b048b1&from=now-6h&to=now&timezone=browser)
* **AWS**:
    * **Queues**:
        * **Courts**:
            * [CPR Court Case Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-prod-cpr_court_cases_queue.fifo&from=now-24h&to=now&timezone=browser)
            * [CPR Court Case Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-prod-cpr_court_cases_dlq.fifo&from=now-24h&to=now&timezone=browser)
        * **Delius**:
            * [Delius Offender Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=Cloudwatch&var-region=default&var-queue=hmpps-person-record-prod-cpr_delius_offender_events_queue)
            * [Delius Offender Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=Cloudwatch&var-region=default&var-queue=hmpps-person-record-prod-cpr_delius_offender_events_dlq)
            * [Delius Merge Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-prod-cpr_delius_merge_events_queue&from=now-24h&to=now)
            * [Delius Merge Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-prod-cpr_delius_merge_events_dlq&from=now-24h&to=now)
            * [Delius Delete Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-prod-cpr_delius_delete_events_queue)
            * [Delius Delete Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-prod-cpr_delius_delete_events_dlq)
        * **Nomis**:
            * [Nomis Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-prod-cpr_nomis_events_queue&from=now-24h&to=now)
            * [Nomis Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-prod-cpr_nomis_events_dlq&from=now-24h&to=now)
            * [Nomis Merge Events](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-prod-cpr_nomis_merge_events_queue&from=now-2d&to=now)
            * [Nomis Merge Events DLQ](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSQS000/aws-sqs?orgId=1&var-datasource=P896B4444D3F0DAB8&var-region=default&var-queue=hmpps-person-record-prod-cpr_nomis_merge_events_dlq&from=now-2d&to=now)
    * **Topics**:
        * [CPR Court Case Topic](https://grafana.live.cloud-platform.service.justice.gov.uk/d/AWSSNS001/aws-sns?from=now-12h&to=now&timezone=browser&var-datasource=P896B4444D3F0DAB8&var-region=default&var-topic=cloud-platform-hmpps-person-record-7fe9dbb1391a89724dd2459be10708c2)

## Logs

How to access the application logs.

### Through Kubernetes

You can read the logs using [kubectl](https://kubernetes.io/docs/reference/kubectl/). 
To view logs from the currently deployed application:

```shell
kubectl logs deployment/hmpps-person-record -n hmpps-person-record-<namespace>
```

### Through AppInsights

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

## How to resolve specific issues
TODO:
