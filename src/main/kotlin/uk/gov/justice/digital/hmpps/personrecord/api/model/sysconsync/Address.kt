package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class Address(
  @Schema(description = "The address ID", example = "9d35c1cb-e515-47fc-b6d1-c776d935a78c")
  val id: String?,
  @Schema(description = "The address type", example = "HOME")
  val type: AddressType,
  @Schema(description = "The address flat detail", example = "12")
  val flat: String?,
  @Schema(description = "The address premise", example = "Brighton Courts")
  val premise: String?,
  @Schema(description = "The address street", example = "Petty France")
  val street: String?,
  @Schema(description = "The address locality", example = "Westminster")
  val locality: String?,
  @Schema(description = "The address town code", example = "LDN")
  val townCode: String?,
  @Schema(description = "The address postcode", example = "LD1 1DN")
  val postcode: String?,
  @Schema(description = "The address county code", example = "WY")
  val countyCode: String?,
  @Schema(description = "The address country code", example = "UK")
  val countryCode: String?,
  @Schema(description = "The address no fixed abode")
  val noFixedAddress: Boolean?,
  @Schema(description = "The address start date", example = "1980-01-01")
  val startDate: LocalDate?,
  @Schema(description = "The address end date", example = "1990-01-01")
  val endDate: String?,
  @Schema(description = "The address comment")
  val comment: String?,
  @Schema(description = "Is this the primary address")
  val isPrimary: Boolean,
  @Schema(description = "Is this the mail address")
  val isMail: Boolean,
  @Schema(description = "Is this an active address")
  val isActive: Boolean,
)

enum class AddressType {
  BUS,
  HOME,
  WORK,
}
