package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema

data class Name(
  @Schema(description = "The title code of the person", example = "MR")
  val titleCode: String?,
  @Schema(description = "The first name of the person", example = "John")
  val firstName: String?,
  @Schema(description = "The middle name of the person", example = "Jimmy")
  val middleName1: String?,
  @Schema(description = "The middle name of the person", example = "Doe")
  val middleName2: String?,
  @Schema(description = "The last name of the person", example = "Smith")
  val lastName: String?,
)
