package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonProperty

data class Descriptor(
  @JsonProperty("code")
  val value: String? = null,
)
