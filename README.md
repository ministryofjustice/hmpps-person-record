# hmpps-person-record
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.message&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-person-record)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-person-record "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-person-record/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-person-record)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-person-record/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-person-record)

### A service for managing identity data about the people we look after in HMPPS

## Prerequisites
- JDK 21 needs to be installed 
- Gradle v8.4 is required for the build


## Running Service Locally

Ensure all docker containers are up and running:

`$ docker compose up -d`

Which should start the following containers: (verify with `$ docker ps` if necessary)
- postgres
- localstack-hmpps-person-record


Start the service ensuring the local spring boot profile is set:

`$ ./gradlew bootRun --args='--spring.profiles.active=local'`

NB. All REST endpoints are secured with the role `ROLE_VIEW_PRISONER_DATA` which will need to be passed to the endpoint as an OAuth token.

## Deployment

Builds and deployments are setup in `Circle CI` and configured in the [config file](./.circleci/config.yml).  
Helm is used to deploy the service to a Kubernetes Cluster using templates in the [`helm_deploy` folder](./helm_deploy).

---

### Testing python locally

my mac only has python3 so:

```
# install and create a virtual environment for python to avoid polluting the python installation https://xkcd.com/1987/
pip3 install virtualenv 
python3 -m venv spike-env 
source spike-env/bin/activate
pip install pandas 
pip install splink

python3 scripts/match.py "$(<scripts/records.json)"
```

This is not suitable for unit testing unfortunately

Running locally:
```
docker build . -t hmpps-person-record   # takes quite a long time but does work
docker run --env SPRING_PROFILES_ACTIVE=local --env DATABASE_ENDPOINT=host.docker.internal:5432 --env HMPPS_SQS_LOCALSTACK_URL=http://host.docker.internal:4566 --env FEATURE_FLAGS_ENABLE_HMCTS_SQS=true --env FEATURE_FLAGS_ENABLE_DELIUS_SEARCH=false --env FEATURE_FLAGS_ENABLE_NOMIS_SEARCH=false hmpps-person-record:latest
```

Useful AWS localstack commands

```
# get all queues
AWS_ACCESS_KEY_ID=key AWS_SECRET_ACCESS_KEY=secret aws sqs list-queues --region=eu-west-2 --endpoint-url=http://localhost:4566
# get queue attributes
AWS_ACCESS_KEY_ID=key AWS_SECRET_ACCESS_KEY=secret aws sqs get-queue-attributes --queue-url=http://localhost:4566/000000000000/cpr_court_case_events_queue --region=eu-west-2 --endpoint-url=http://localhost:4566
# publish sample court case message for matching
# NB get topic-arn from output at startup - look for this in the logs
# Created a LocalStack SNS topic for topicId courtcaseeventstopic with ARN
# run this twice to run the splink matcher
 AWS_ACCESS_KEY_ID=key AWS_SECRET_ACCESS_KEY=secret aws --region=eu-west-2  --endpoint-url=http://localhost:4566 sns publish \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:8087b158-cc2c-448f-a49e-f11f8fd05593 \
    --message-attributes '{"messageType": {"DataType": "String","StringValue": "COMMON_PLATFORM_HEARING"},"source": {"DataType": "String","StringValue": "delius"},"id": {"DataType": "String","StringValue": "fcf89ef7-f6e8-ee95-326f-8ce87d3b8ea0"},"contentType": {"DataType": "String","StringValue": "text/plain;charset=UTF-8"},"timestamp": {"DataType": "Number","StringValue": "1611149702333"}}' \
    --message "$(<hmpps_person_record_python/sample-court-case-event.json)"
```