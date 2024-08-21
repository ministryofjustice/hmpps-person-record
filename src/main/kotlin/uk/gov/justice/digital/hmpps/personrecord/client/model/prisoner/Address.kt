package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class Address(
  @JsonProperty("postalCode")
  val postcode: String? = null,
  val fullAddress: String? = null,
  @JsonProperty("noFixedAddress")
  val noFixedAbode: Boolean? = null,
  val startDate: LocalDate? = null,
)
