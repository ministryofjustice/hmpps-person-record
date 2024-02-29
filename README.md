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
source spike-env/activate
pip install pandas 
pip install splink

python3 scripts/match.py "$(<scripts/records.json)"
```