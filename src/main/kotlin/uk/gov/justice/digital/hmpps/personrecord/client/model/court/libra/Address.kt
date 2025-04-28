package uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Address(
  @JsonProperty("pcode")
  val postcode: String? = null,

  @JsonProperty("line1")
  val buildingName: String? = null,

  @JsonProperty("line2")
  val buildingNumber: String? = null,

  @JsonProperty("line3")
  val thoroughfareName: String? = null,

  @JsonProperty("line4")
  val dependentLocality: String? = null,

  @JsonProperty("line5")
  val postTown: String? = null,
)
