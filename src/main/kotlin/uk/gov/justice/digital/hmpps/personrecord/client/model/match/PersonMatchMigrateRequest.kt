package uk.gov.justice.digital.hmpps.personrecord.client.model.match

data class PersonMatchMigrateRequest(
  val records: List<PersonMatchRecord> = listOf(),
)
