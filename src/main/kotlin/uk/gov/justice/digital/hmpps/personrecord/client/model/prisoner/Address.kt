package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Address(
  @JsonProperty("postalCode")
  val postcode: String? = null,
  val addressType: String? = null,
  @JsonProperty("fullAddress")
  val fullAddress: String? = null,
)
