package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent

data class PersonReference(
  val identifiers: List<PersonIdentifier>? = emptyList(),
)
