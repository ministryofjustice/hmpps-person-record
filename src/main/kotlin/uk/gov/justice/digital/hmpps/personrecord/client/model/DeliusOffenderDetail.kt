package uk.gov.justice.digital.hmpps.personrecord.client.model

import java.time.LocalDate

data class DeliusOffenderDetail(
  val name: Name,
  val identifiers: Identifiers,
  val dateOfBirth: LocalDate? = null,

)
