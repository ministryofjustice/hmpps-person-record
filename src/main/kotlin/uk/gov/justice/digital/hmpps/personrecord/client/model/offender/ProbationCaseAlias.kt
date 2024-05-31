package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import java.time.LocalDate

data class ProbationCaseAlias(
  val name: Name,
  val dateOfBirth: LocalDate? = null,
)
