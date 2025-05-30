package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs

data class LargeMessageBody(val s3Key: String, val s3BucketName: String)
