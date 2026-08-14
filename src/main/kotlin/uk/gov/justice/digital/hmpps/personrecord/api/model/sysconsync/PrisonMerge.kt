package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema

data class PrisonMerge(
  @Schema(description = "The prison number of the person being merged from", example = "A1234BC")
  val fromPrisonNumber: String,
)
