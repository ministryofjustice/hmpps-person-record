package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationCase(
  val name: ProbationCaseName,
  val identifiers: Identifiers,
  val title: ProbationCaseTitle? = null,
  val dateOfBirth: LocalDate? = null,
  val aliases: List<ProbationCaseAlias>? = emptyList(),
  val contactDetails: ProbationCaseContactDetails? = null,
  val addresses: List<ProbationCaseAddress> = emptyList(),
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

data class ProbationCaseTitle(
  @JsonProperty("code")
  val value: String? = null,
)

data class ProbationCaseContactDetails(
  val telephone: String? = null,
  val mobile: String? = null,
  val email: String? = null,
)

data class ProbationCaseAddress(
  val postcode: String? = null,
)
