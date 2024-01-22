
## Accessing AWS Resources via the AWS CLI


### Setting the kubenetes namespace for the environment

The k8s namespace can either be passed in as variable on the command line or by amending the bash script and
setting the namespace variable to one of:

- hmpps-person-record-dev
- hmpps-person-record-preprod
- hmpps-person-record-prod 


### Creating the kubenetes service pod

To run the `setup-service-pod.bash` script:

`$ ./setup-service-pod.bash`

To run the script supplying the namespace:

`$ ./setup-service-pod.bash --namespace hmpps-person-record-preprod`


This will create a k8s service pod that allows access to AWS resources using the AWS Service Account
associated with the hmpps-person-record-dev/preprod/prod cluster.

The script sets up some convenience environment variables to facilitate testing:

- RDS_INSTANCE_IDENTIFIER
- COURT_CASE_EVENTS_QUEUE_URL
- COURT_CASE_EVENTS_DLQ_URL
- DELIUS_OFFENDER_EVENTS_QUEUE_URL
- DELIUS_OFFENDER_EVENTS_DLQ_URL
 
After running the script you'll find yourself automatically in the service pod and can run AWS CLI commands as if you are authenticated with the service account.


### Example usages:

Count the number of messages on the Court case events queues:

`$ aws sqs get-queue-attributes --queue-url $COURT_CASE_EVENTS_QUEUE_URL --attribute-names ApproximateNumberOfMessages`

`$ aws sqs get-queue-attributes --queue-url $COURT_CASE_EVENTS_DLQ_URL --attribute-names ApproximateNumberOfMessages`

List the known RDS Database snapshots:

`$ aws rds describe-db-snapshots --db-instance-identifier "$INSTANCE_IDENTIFIER"`

Purge the court case events queue:

`$ aws sqs purge-queue --queue-url $COURT_CASE_EVENTS_QUEUE_URL`

Put a message onto the court case events queue for testing purposes:

`$ aws sqs send-message --endpoint-url https://sqs.eu-west-2.amazonaws.com --queue-url $COURT_CASE_EVENTS_QUEUE_URL --message-body "{ some json }"`

Retrieve a message from the Court case events DLQ and save to a file called dlq_data:

`$ aws sqs receive-message --queue-url $COURT_CASE_EVENTS_DLQ_URL > "dlq_data"`


### Sample Court Case Events data:

Example Libra/Common platform test data can be found here : https://github.com/ministryofjustice/hmpps-probation-in-court-utils/tree/main/cases
