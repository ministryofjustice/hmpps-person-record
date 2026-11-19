package uk.gov.justice.digital.hmpps.personrecord.api.model.search

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class ExactMatchSearchRequest(
  @NotBlank val firstName: String,
  @NotBlank val lastName: String,
  val dateOfBirth: LocalDate,
  @NotBlank val pnc: String,
)
