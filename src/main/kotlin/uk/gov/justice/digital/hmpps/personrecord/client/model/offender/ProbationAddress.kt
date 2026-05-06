package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationAddress(
  val noFixedAbode: Boolean? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
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
  val status: ProbationAddressStatus? = null,
  val type: ProbationAddressType? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationAddressStatus(
  val code: String? = null,
  val description: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationAddressType(
  val code: String? = null,
  val description: String? = null,
)
