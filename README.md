# hmpps-person-record
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.message&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-person-record)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-person-record "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-person-record/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-person-record)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-person-record/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-person-record)

### A service for managing identity data about the people we look after in HMPPS

## Prerequisites
- JDK 21

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

1. Pause message consumption by adding the profile `seeding` to the spring configuration in helm
2. Delete all data with source system of NOMIS or DELIUS as appropriate
    At the moment we have no delete cascade on the child tables, so we have to do this to avoid orphaned records
```
    delete from personrecordservice.address a where a.fk_person_id in (select id from personrecordservice.person p where p.source_system = 'DELIUS')
    delete from personrecordservice.alias a where a.fk_person_id in (select id from personrecordservice.person p where p.source_system = 'DELIUS')
    delete from personrecordservice.contact a where a.fk_person_id in (select id from personrecordservice.person p where p.source_system = 'DELIUS')
    delete from personrecordservice.person p where p.source_system = 'DELIUS'
```    
3. Get a shell on the hmpps-person-record pod (this is for preproduction):
```
kubectl exec -it deployment/hmpps-person-record -n hmpps-person-record-preprod -- bash

# takes 90 minutes -3 hours
curl -i -X POST http://localhost:8080/populatefromprison 

# takes 7-8 hours
curl -i -X POST http://localhost:8080/populatefromprobation
```
4. remove the profile `seeding` to resume message consumption

### importing cluster data manually
#### Creating the tables
Connect to the database [using the process defined](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/other-topics/rds-external-access.html)

Copy and paste the SQL from  [CPR-356.sql](./src/main/resources/db/scripts/CPR-356.sql)
#### data import
A csv file will be supplied by Data Science, possibly by encrypted file transfer. Compare the column names with the table splink_cluster as defined in [CPR-356.sql](./src/main/resources/db/scripts/CPR-356.sql) and amend the csv file to match the column names. For example, `rename offender_id_display` to `prisoner_number`
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

