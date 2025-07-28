package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class Sentence(
  @Schema(description = "The sentence date", example = "1980-01-01")
  val sentenceDate: LocalDate?,
)
