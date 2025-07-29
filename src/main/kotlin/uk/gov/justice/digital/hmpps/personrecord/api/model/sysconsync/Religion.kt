package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema

data class Religion(
  @Schema(description = "The religion ID", example = "7b0c9e8a-bca7-4bd1-890c-2f42e69d3bcf")
  val id: String?,
  @Schema(description = "The religion code")
  val religionCode: String?,
)
