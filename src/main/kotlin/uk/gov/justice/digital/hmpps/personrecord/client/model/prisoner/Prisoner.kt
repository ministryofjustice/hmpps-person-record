package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.personrecord.model.PersonAlias
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class Prisoner(
  @NotBlank
  @JsonProperty("prisonerNumber")
  val prisonNumber: String,
  val firstName: String,
  val middleNames: String,
  val lastName: String,
  @JsonProperty("pncNumberCanonicalLong")
  val pnc: String,
  @JsonProperty("croNumber")
  val cro: String,
  val dateOfBirth: LocalDate,
  val aliases: List<PersonAlias>,
)
