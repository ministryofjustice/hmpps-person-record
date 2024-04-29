package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.validation.constraints.NotBlank

@JsonIgnoreProperties(ignoreUnknown = true)
data class Prisoner(
  @NotBlank
  val prisonerNumber: String,
  val firstName: String,

)
