package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class DemographicAttributes(
  @Schema(description = "The person's date of birth", example = "1980-01-01")
  val dateOfBirth: LocalDate?,
  @Schema(description = "The person's birth place", example = "Milton Keynes")
  val birthPlace: String?,
  @Schema(description = "The person's birth country code", example = "UK")
  val birthCountryCode: String?,
  @Schema(description = "The person's nationality code", example = "BR")
  val nationalityCode: String?,
  @Schema(description = "The person's multiple nationalities")
  val multipleNationalities: String?,
  @Schema(description = "The person's ethnicity code")
  val ethnicityCode: String?,
  @Schema(description = "The person's sexual orientation code")
  val sexualOrientationCode: String?,
  @Schema(description = "The person's sex code")
  val sexCode: String?,
  @Schema(description = "The person's disability status")
  val disability: Boolean?,
  @Schema(description = "The person's interest to immigration status")
  val interestToImmigration: Boolean?,
)
