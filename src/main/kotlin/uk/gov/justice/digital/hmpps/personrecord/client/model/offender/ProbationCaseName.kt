package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonProperty

data class ProbationCaseName(
  @JsonProperty("forename")
  val firstName: String? = null,
  @JsonProperty("surname")
  val lastName: String? = null,
  @JsonProperty("middleName")
  val middleNames: String? = null,
)
