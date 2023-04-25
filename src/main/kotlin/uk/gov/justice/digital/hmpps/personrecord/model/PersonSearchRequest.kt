package uk.gov.justice.digital.hmpps.personrecord.model

import java.time.LocalDate

data class PersonSearchRequest(
  val pncNumber: String? = null,
  val crn: String? = null,
  val forename: String? = null,
  val middleNames: String? = null,
  val surname: String,
  val dateOfBirth: LocalDate? = null,
)
