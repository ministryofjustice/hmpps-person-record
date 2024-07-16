package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent

data class PersonReference(
  val identifiers: List<PersonKey>? = emptyList(),
)
