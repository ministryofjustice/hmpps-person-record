package uk.gov.justice.digital.hmpps.personrecord.client.model

data class Identifiers(
  val crn: String,
  val pnc: String? = null,
)
