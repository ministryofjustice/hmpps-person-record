package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class Prisoner(
  @NotBlank
  val prisonerNumber: String,
  val pncNumber: String? = null,
  val croNumber: String? = null,
  val firstName: String? = null,
  val middleNames: String? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val gender: String? = null,
  val nationality: String? = null,
)
