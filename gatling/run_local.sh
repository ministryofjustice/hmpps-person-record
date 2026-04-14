#!/bin/bash

echo "Fetch DB details"
DB_JSON=$(cloud-platform decode-secret --secret=hmpps-person-record-rds-instance-output --namespace=hmpps-person-record-dev)

export DB_NAME=$(echo "$DB_JSON" | jq -r '.data.database_name')
export DB_URL="jdbc:postgresql://localhost:5432/$DB_NAME"
export DB_USER=$(echo "$DB_JSON" | jq -r '.data.database_username')
export DB_PASS=$(echo "$DB_JSON" | jq -r '.data.database_password')

sleep 5
echo "DB variables loaded"

echo "Generate test data"
export OUT_FILE="src/gatling/resources/testdata/data.csv"
./gradlew generateTestData  --args="'$DB_URL' '$DB_USER' '$DB_PASS' '$OUT_FILE'"

echo "Running Gatling..."
export GATLING_CLIENT_ID="<CLIENT_ID>"
export GATLING_CLIENT_SECRET='<CLIENT_SECRET>'
CLIENT_ID=$GATLING_CLIENT_ID CLIENT_SECRET=$GATLING_CLIENT_SECRET ./gradlew gatlingRun -Denv=dev -Dprofile=local

echo "Open Gatling Report"
open "build/reports/gatling/$(ls -1t build/reports/gatling | head -1)/index.html"


