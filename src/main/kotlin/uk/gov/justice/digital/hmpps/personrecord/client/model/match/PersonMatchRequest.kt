package uk.gov.justice.digital.hmpps.personrecord.client.model.match

data class PersonMatchRequest(
  val records: List<PersonMatchRecord> = listOf(),
)
