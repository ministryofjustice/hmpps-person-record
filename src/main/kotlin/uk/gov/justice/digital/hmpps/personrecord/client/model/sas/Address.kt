package uk.gov.justice.digital.hmpps.personrecord.client.model.sas

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class SasGetAddressResponse(
  val data: SasAddressData,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SasAddressData(
  val crn: String,
  @JsonProperty("id")
  val cprAddressId: String,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val address: Address,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Address(
  val postcode: String? = null,
  val subBuildingName: String? = null,
  val buildingName: String? = null,
  val buildingNumber: String? = null,
  val thoroughfareName: String? = null,
  val dependentLocality: String? = null,
  val postTown: String? = null,
  val county: String? = null,
  val country: String? = null,
  val uprn: String? = null,
)
