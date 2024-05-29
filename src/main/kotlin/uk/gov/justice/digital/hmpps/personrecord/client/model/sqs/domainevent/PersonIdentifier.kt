package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent

data class PersonIdentifier(
  val type: String,
  val value: String,
)
