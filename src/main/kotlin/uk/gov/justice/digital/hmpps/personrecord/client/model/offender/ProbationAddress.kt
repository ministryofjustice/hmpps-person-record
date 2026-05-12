package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationAddress(
  val noFixedAbode: Boolean? = null,
  val startDateTime: OffsetDateTime? = null,
  val endDateTime: OffsetDateTime? = null,
  val postcode: String? = null,
  val fullAddress: String? = null,
  val buildingName: String? = null,
  val addressNumber: String? = null,
  val streetName: String? = null,
  val district: String? = null,
  val townCity: String? = null,
  val county: String? = null,
  val uprn: String? = null,
  val notes: String? = null,
  val telephoneNumber: String? = null,
  @JsonProperty("id")
  val deliusAddressId: Long? = null,
  @JsonProperty("typeVerified")
  val isVerified: Boolean? = null,
  @JsonProperty("type")
  val usage: ProbationAddressUsage? = null,
  val status: ProbationAddressStatus? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationAddressStatus(
  val code: String,
  val description: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationAddressUsage(
  val code: String,
  val description: String,
)
