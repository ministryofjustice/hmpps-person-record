package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonProperty

data class Value(
  @JsonProperty("code")
  val value: String? = null,
)
