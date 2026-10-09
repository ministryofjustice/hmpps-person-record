package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import java.time.LocalDate

@Schema(description = "Create a prisoner address")
data class PrisonCreateAddressRequest(
  @Schema(description = "The address sub building name. Previously `flat` in Prison API", example = "3B")
  val subBuildingName: String? = null,

  @Schema(description = "The address building number. Previously `premise` in Prison API", example = "Liverpool Prison")
  val buildingNumber: String? = null,

  @Schema(description = "The address thoroughfare name. Previously `street` in Prison API", example = "Slinn Street")
  val thoroughfareName: String? = null,

  @Schema(description = "The address dependent locality. Previously `locality` in Prison API", example = "Brincliffe")
  val dependentLocality: String? = null,

  @Schema(
    description = "The address post town. Previously `townCode` in Prison API - send the description of the CITY reference data rather than the code",
    example = "Liverpool",
  )
  val postTown: String? = null,

  @Schema(
    description = "The address county. Previously `countyCode` in Prison API - send the description of the COUNTY reference data rather than the code",
    example = "Herefordshire",
  )
  val county: String? = null,

  @Schema(requiredMode = REQUIRED, description = "Country code. Note: Reference domain is COUNTRY. Unchanged from `countryCode` in Prison API", example = "ENG")
  val countryCode: CountryCode,

  @Schema(description = "The address postcode. Previously `postalCode` in Prison API", example = "LI1 5TH")
  val postcode: String? = null,

  @Schema(description = "The address status code. `primary` in Prison API is M, `mail` in Prison API is MA and both selected is PM.", example = "M", required = true)
  val statusCode: AddressStatusCode,

  @Schema(description = "Is the person without a permanent residence. Previously `noFixedAddress` in Prison API", example = "false")
  val noFixedAbode: Boolean? = null,

  @Schema(requiredMode = REQUIRED, description = "Date address is in use from.", example = "2005-05-12")
  val startDate: LocalDate,

  @Schema(description = "Date address is in use to.", example = "2005-05-12")
  val endDate: LocalDate? = null,

  @Schema(requiredMode = REQUIRED, description = "List of address usage codes. Previously `addressUsages` in Prison API", example = "[\"HOME\"]")
  val usages: Collection<AddressUsageCode>,

  @Schema(description = "The user who made the change")
  val userId: String,
)
