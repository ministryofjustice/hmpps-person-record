package uk.gov.justice.digital.hmpps.personrecord.client.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrisonerNumbers(

  val totalPages: Int,
  val first: Boolean,
  val last: Boolean,
  @JsonProperty("content")
  val numbers: List<String>,
)
