#!/bin/bash
namespace=hmpps-person-record-dev
debug_pod_name=hmpps-person-record-utils

# Read any named params
while [ $# -gt 0 ]; do

   if [[ $1 == *"--"* ]]; then
        param="${1/--/}"
        declare $param="$2"
   fi

  shift
done

set -o history -o histexpand
set -e
exit_on_error() {
    exit_code=$1
    last_command=${@:2}
    if [ $exit_code -ne 0 ]; then
        >&2 echo "💥 Last command:"
        >&2 echo "    \"${last_command}\""
        >&2 echo "❌ Failed with exit code ${exit_code}."
        >&2 echo "🟥 Aborting"
        exit $exit_code
    fi
}

echo "🔑 Getting Person Record RDS instance from secrets ..."
secret_json=$(cloud-platform decode-secret -s hmpps-person-record-rds-instance-output -n $namespace --skip-version-check)
export RDS_INSTANCE_IDENTIFIER=$(echo "$secret_json" | jq -r .data.rds_instance_address | sed s/[.].*//)

echo "🔑 Getting court case events queue url from secrets..."
secret_json=$(cloud-platform decode-secret -s sqs-cpr-court-case-events-secret -n $namespace --skip-version-check)
export COURT_CASE_EVENTS_QUEUE_URL=$(echo "$secret_json" | jq -r .data.sqs_queue_url)

echo "🔑 Getting court case events DLQ url from secrets..."
secret_json=$(cloud-platform decode-secret -s sqs-cpr-court-case-events-dlq-secret -n $namespace --skip-version-check)
export COURT_CASE_EVENTS_DLQ_QUEUE_URL=$(echo "$secret_json" | jq -r .data.sqs_queue_url)

echo "🔑 Getting delius offender events queue url from secrets..."
secret_json=$(cloud-platform decode-secret -s sqs-cpr-delius-offender-events-secret -n $namespace --skip-version-check)
export DELIUS_OFFENDER_EVENTS_QUEUE_URL=$(echo "$secret_json" | jq -r .data.sqs_queue_url)

echo "🔑 Getting delius offender events DLQ url from secrets..."
secret_json=$(cloud-platform decode-secret -s sqs-cpr-delius-offender-events-dlq-secret -n $namespace --skip-version-check)
export DELIUS_OFFENDER_EVENTS_DLQ_QUEUE_URL=$(echo "$secret_json" | jq -r .data.sqs_queue_url)

kubectl --namespace=$namespace --request-timeout='120s' run \
    --env "namespace=$namespace" \
    --env "RDS_INSTANCE_IDENTIFIER=$RDS_INSTANCE_IDENTIFIER" \
    --env "COURT_CASE_EVENTS_QUEUE_URL=$COURT_CASE_EVENTS_QUEUE_URL" \
    --env "COURT_CASE_EVENTS_DLQ_QUEUE_URL=$COURT_CASE_EVENTS_DLQ_QUEUE_URL" \
    --env "DELIUS_OFFENDER_EVENTS_QUEUE_URL=$DELIUS_OFFENDER_EVENTS_QUEUE_URL" \
    --env "DELIUS_OFFENDER_EVENTS_DLQ_QUEUE_URL=$DELIUS_OFFENDER_EVENTS_DLQ_QUEUE_URL" \
  -it --rm $debug_pod_name --image=quay.io/hmpps/hmpps-probation-in-court-utils:latest \
   --restart=Never --overrides='{ "spec": { "serviceAccount": "person-record-service" } }'


