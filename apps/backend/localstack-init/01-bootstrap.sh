#!/bin/bash
# LocalStack ready hook: provisions DynamoDB tables + SQS queues for local `docker compose up`.
# Runs inside the LocalStack container (awslocal auto-targets http://localhost:4566).
set -euo pipefail

echo "[bootstrap] creating DynamoDB tables..."
awslocal dynamodb create-table \
  --table-name urls \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

awslocal dynamodb create-table \
  --table-name statistics \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

echo "[bootstrap] seeding global statistics row..."
awslocal dynamodb put-item \
  --table-name statistics \
  --item '{"id":{"S":"global"},"totalLinks":{"N":"0"},"totalClicks":{"N":"0"}}'

echo "[bootstrap] creating SQS dead-letter queue..."
awslocal sqs create-queue --queue-name link-events-dlq

DLQ_URL=$(awslocal sqs get-queue-url --queue-name link-events-dlq --query QueueUrl --output text)
DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$DLQ_URL" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' --output text)

# RedrivePolicy is a JSON string nested inside the attributes JSON object.
ATTRS=$(printf '{"VisibilityTimeout":"30","RedrivePolicy":"{\\"deadLetterTargetArn\\":\\"%s\\",\\"maxReceiveCount\\":\\"5\\"}"}' "$DLQ_ARN")

echo "[bootstrap] creating SQS work queues..."
awslocal sqs create-queue --queue-name link-created --attributes "$ATTRS"
awslocal sqs create-queue --queue-name link-forwarded --attributes "$ATTRS"

echo "[bootstrap] complete."
