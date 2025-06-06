# hmpps-person-record
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.message&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-person-record)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-person-record "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-person-record/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-person-record)

## About

HMPPS Person Record is a service for managing identity data about the people we look after in HMPPS. 
Dealing with record updates and creation, as well detecting / preventing any duplicate person records they may have been created.

## Service

For any service enquires / troubleshooting see the [runbook](./runbooks/000-Person-Record.md).

## Development

### Prerequisites
- JDK 21

### Running tests
```
$ make test
```

### Deployment

Builds and deployments are set up in `Circle CI` and configured in the [config file](./.circleci/config.yml).  
Helm is used to deploy the service to a Kubernetes Cluster using templates in the [`helm_deploy` folder](./helm_deploy).

### Running Service Locally

Mostly runs against dev services, uses localstack for the queues

`$ make run-local`

### process a Common Platform message when running locally

```shell
AWS_REGION=eu-west-2 AWS_ACCESS_KEY_ID=key AWS_SECRET_ACCESS_KEY=secret aws --endpoint-url=http://localhost:4566 sns publish \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:courtcasestopic \
    --message-attributes file://$(pwd)/src/test/resources/examples/commonPlatformMessageAttributes.json \
    --message file://$(pwd)/src/test/resources/examples/commonPlatformMessage.json
```

### Working with AWS resources

You can retrieve the queue URLs from the secrets like this (preprod court cases queue URL):
`cloud-platform decode-secret -n hmpps-person-record-preprod -s sqs-cpr-court-cases-secret | jq -r '.data.sqs_queue_url'`

We can access queues, topics and all other AWS dependencies using the [service pod provided by cloud-platform](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/other-topics/cloud-platform-service-pod.html). 
There is currently an instance running in preprod and prod - see the link above for instructions on how to get a shell on it.

Then you can run AWS CLI commands on the pod like this:

`aws sqs get-queue-attributes --queue-url <COURT_CASES_QUEUE_URL> --attribute-names ApproximateNumberOfMessages`


#### Localstack command to create FIFO topic

`AWS_REGION=eu-west-2 AWS_ACCESS_KEY_ID=key AWS_SECRET_ACCESS_KEY=secret aws --endpoint-url=http://localhost:4566 sns create-topic --name courteventstopic.fifo --attributes '{"FifoTopic": "true", "ContentBasedDeduplication": "true"}'`

`AWS_REGION=eu-west-2 AWS_ACCESS_KEY_ID=key AWS_SECRET_ACCESS_KEY=secret aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name cpr_court_events_queue.fifo --attributes '{"FifoQueue": "true", "ContentBasedDeduplication": "true"}'`

`AWS_REGION=eu-west-2 AWS_ACCESS_KEY_ID=key AWS_SECRET_ACCESS_KEY=secret aws --endpoint-url=http://localhost:4566 sns subscribe --topic-arn arn:aws:sns:eu-west-2:000000000000:courteventstopic.fifo --protocol sqs --notification-endpoint arn:aws:sqs:eu-west-2:000000000000:cpr_court_events_queue.fifo`

