package uk.gov.justice.digital.hmpps.personrecord.client.model

import java.time.LocalDate

data class SearchDto(
  val firstName: String? = null,
  val surname: String? = null,
  val dateOfBirth: LocalDate? = null,
  val pncNumber: String? = null,
  val croNumber: String? = null,
  val crn: String? = null,
  val nomsNumber: String? = null,
  val includeAliases: Boolean? = false,
)
