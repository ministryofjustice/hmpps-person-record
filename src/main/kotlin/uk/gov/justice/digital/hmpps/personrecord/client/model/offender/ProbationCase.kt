package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationCase(
  val name: ProbationCaseName,
  val identifiers: Identifiers,
  val dateOfBirth: LocalDate? = null,
  val aliases: List<ProbationCaseAlias>? = emptyList(),
)

data class ProbationCaseName(
  @JsonProperty("forename")
  val firstName: String? = null,
  @JsonProperty("surname")
  val lastName: String? = null,
  @JsonProperty("middleName")
  val middleNames: String? = null,
)

data class ProbationCaseAlias(
  val name: ProbationCaseName,
  val dateOfBirth: LocalDate? = null,
)
