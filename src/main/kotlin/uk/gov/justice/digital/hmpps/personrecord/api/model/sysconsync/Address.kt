package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class Address(
  @Schema(description = "The full address", example = "Sub building 2, Main Building, 102 Petty France, Westminster, London, Greater London, SW1H 9AJ")
  val fullAddress: String? = null,
  @Schema(description = "Is the person without a permanent residence", example = "false")
  val noFixedAbode: Boolean? = null,
  @Schema(description = "The address start date", example = "2020-02-26")
  val startDate: LocalDate? = null,
  @Schema(description = "The address end date", example = "2023-07-15")
  val endDate: LocalDate? = null,
  @Schema(description = "The address postcode", example = "SW1H 9AJ")
  val postcode: String? = null,
  @Schema(description = "The address sub building name", example = "Sub building 2")
  val subBuildingName: String? = null,
  @Schema(description = "The address building name", example = "Main Building")
  val buildingName: String? = null,
  @Schema(description = "The address building number", example = "102")
  val buildingNumber: String? = null,
  @Schema(description = "The address thoroughfare name", example = "Petty France")
  val thoroughfareName: String? = null,
  @Schema(description = "The address dependent locality", example = "Westminster")
  val dependentLocality: String? = null,
  @Schema(description = "The address building number", example = "London")
  val postTown: String? = null,
  @Schema(description = "The address building number", example = "Greater London")
  val county: String? = null,
  @Schema(description = "The address country code", example = "UK")
  val countryCode: String? = null,
  @Schema(description = "The address comment", example = "String")
  val comment: String? = null,

  // NOTE: The below don't exist on Person. We are going to ignore them?!?
  @Schema(description = "Is this the primary address", example = "true")
  val isPrimary: Boolean? = null,
  @Schema(description = "Is this the mail address", example = "true")
  val isMail: Boolean? = null,
  @Schema(description = "List of address usages")
  val addressUsage: List<AddressUsage> = emptyList(),
)
