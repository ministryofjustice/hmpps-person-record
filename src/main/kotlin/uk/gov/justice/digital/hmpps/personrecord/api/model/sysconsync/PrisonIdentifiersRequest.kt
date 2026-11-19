package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid

data class PrisonIdentifiersRequest(
  @Schema(description = "The full set of identifiers for the nomis offender id. Any existing identifiers not in this list will be removed")
  @Valid
  val identifiers: List<PrisonIdentifier>,
)
