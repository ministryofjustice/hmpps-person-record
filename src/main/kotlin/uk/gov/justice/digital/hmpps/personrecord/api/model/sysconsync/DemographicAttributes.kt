package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import java.time.LocalDate

data class DemographicAttributes(
  @Schema(description = "The person's date of birth", example = "1980-01-01")
  val dateOfBirth: LocalDate? = null,
  @Schema(description = "The person's birth place", example = "Milton Keynes")
  val birthPlace: String? = null,
  @Schema(description = "The person's birth country code", example = "UK")
  val birthCountryCode: String? = null,
  @Schema(description = "The person's ethnicity code", example = "W1")
  val ethnicityCode: String? = null,
  @Schema(description = "The person's sex code", example = "M")
  val sexCode: SexCode? = null,
  @Schema(description = "The person's sexual orientation code", example = "HET")
  val sexualOrientation: String? = null,
  @Schema(description = "Does the person have a disability", example = "false")
  val disability: Boolean? = null,
  @Schema(description = "Is the person of interest to immigration", example = "false")
  val interestToImmigration: Boolean? = null,
  @Schema(description = "The person's religion code", example = "MOS")
  val religionCode: String? = null,
  @Schema(description = "The person's nationality code", example = "GB")
  val nationalityCode: String? = null,
  @Schema(description = "The person's nationality notes", example = "Possibly Finnish")
  val nationalityNote: String? = null,
)
