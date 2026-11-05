package uk.gov.justice.digital.hmpps.personrecord.api.model.search

import java.time.LocalDate

data class ExactMatchSearchRequest(
  val firstName: String,
  val lastName: String,
  val dateOfBirth: LocalDate,
  val pnc: String,
)
