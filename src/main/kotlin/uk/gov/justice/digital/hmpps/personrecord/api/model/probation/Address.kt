package uk.gov.justice.digital.hmpps.personrecord.api.model.probation

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import java.time.ZonedDateTime

@Schema(name = "ProbationCreateAddress")
data class Address(
  @Schema(description = "Is the person without a permanent residence", example = "false", required = true)
  val noFixedAbode: Boolean,
  @Schema(description = "The address start date", example = "2026-05-15T11:08:46.347Z", required = true)
  val startDate: ZonedDateTime,
  @Schema(description = "The address end date", example = "2026-05-15T11:08:46.347Z")
  val endDate: ZonedDateTime? = null,
  @Schema(description = "The address postcode", example = "SW1H 9AJ")
  val postcode: String? = null,
  @Schema(description = "The address unique property reference number", example = "100120991537")
  val uprn: String? = null,
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
  @Schema(description = "The address country code", example = "GBR")
  val countryCode: CountryCode? = null,
  @Schema(description = "The address comment", example = "String")
  val comment: String? = null,
  @Schema(description = "The address status code", example = "M", required = true)
  val statusCode: AddressStatusCode,
  @Schema(description = "Is the address type verified", example = "false", required = true)
  val typeVerified: Boolean = false,
  @Schema(description = "List of address usages", required = true)
  val usages: List<AddressUsage> = emptyList(),
  @Schema(description = "List of address contacts")
  val contacts: List<AddressContact> = emptyList(),
)
