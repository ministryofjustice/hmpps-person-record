#!/usr/bin/env bash
set -e
export TERM=ansi
export AWS_ACCESS_KEY_ID=foobar
export AWS_SECRET_ACCESS_KEY=foobar
export AWS_DEFAULT_REGION=eu-west-2
export PAGER=

# Create the bucket
aws s3 --endpoint-url=http://localhost:4566 --region eu-west-2 ls s3://local-court-message-bucket || aws --endpoint-url=http://localhost:4566 --region=eu-west-2 s3 mb s3://local-court-message-bucket
aws s3 --endpoint-url=http://localhost:4566 --region eu-west-2 ls s3://cpr-local-court-message-bucket || aws --endpoint-url=http://localhost:4566 --region=eu-west-2 s3 mb s3://cpr-local-court-message-bucket

echo "S3 Configured"

