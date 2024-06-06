# hmpps-person-record
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.message&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-person-record)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-person-record "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-person-record/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-person-record)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-person-record/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-person-record)

### A service for managing identity data about the people we look after in HMPPS

## Prerequisites
- JDK 21 needs to be installed

## Running tests
```
$ make test
```

## Running Service Locally

Mostly runs against dev services, uses localstack for the queues

Ensure all docker containers are up and running:

`$ make start-containers`

Which should start the following containers: (verify with `$ docker ps` if necessary)
- postgres
- localstack-hmpps-person-record

To stop the containers:

```
$ make stop-containers
```

Start the service ensuring the local spring boot profile is set:

`$ ./gradlew bootRun --args='--spring.profiles.active=local'`

## Seeding data

Pause message consumption (scale pods down to 0 and silence alerts)
Delete all data with source system of NOMIS or DELIUS as appropriate

Get a shell on the hmpps-person-record pod (this is for dev):
```kubectl exec -it deployment/hmpps-person-record -n hmpps-person-record-dev -- bash

# takes 2-3 hours
curl -i -X POST http://localhost:8080/populatefromprison 

# takes 7-8 hours
curl -i -X POST http://localhost:8080/populatefromprobation
```

## process a Common Platform message

```shell
AWS_REGION=eu-west-2 AWS_ACCESS_KEY_ID=key AWS_SECRET_ACCESS_KEY=secret aws --endpoint-url=http://localhost:4566 sns publish \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:courtcaseeventstopic \
    --message-attributes file://$(pwd)/src/test/resources/examples/commonPlatformMessageAttributes.json \
    --message file://$(pwd)/src/test/resources/examples/commonPlatformMessage.json
```

## Deployment

Builds and deployments are set up in `Circle CI` and configured in the [config file](./.circleci/config.yml).  
Helm is used to deploy the service to a Kubernetes Cluster using templates in the [`helm_deploy` folder](./helm_deploy).

---

