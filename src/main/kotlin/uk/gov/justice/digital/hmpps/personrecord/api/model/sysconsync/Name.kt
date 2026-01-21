package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema

data class Name(
  @Schema(description = "The title code of the person", example = "MR")
  val titleCode: String? = null,
  @Schema(description = "The first name of the person", example = "John")
  val firstName: String? = null,
  @Schema(description = "The middle names of the person", example = "Jimmy Jane")
  val middleNames: String? = null,
  @Schema(description = "The last name of the person", example = "Smith")
  val lastName: String? = null,
)
