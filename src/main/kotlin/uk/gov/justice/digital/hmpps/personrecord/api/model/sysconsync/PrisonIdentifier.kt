package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import java.time.LocalDate

data class PrisonIdentifier(
  @Schema(description = "The nomis identifier id")
  val nomisIdentifierId: NomisIdentifierId,
  @Schema(description = "The identifier type", example = "PNC")
  val type: IdentifierType,
  @Schema(description = "The identifier value", example = "2000/1234567A")
  val value: String,
  @Schema(description = "The identifier comment", example = "Some comment")
  val comment: String? = null,
  @Schema(description = "The issued date")
  val issuedDate: LocalDate?,
  @Schema(description = "Verified")
  val verified: Boolean,

)

data class NomisIdentifierId(
  @Schema(description = "The nomis offender id for this identifier", example = "10000")
  val nomisOffenderId: Long,
  @Schema(description = "The sequence index for the identifier on the offender", example = "1")
  val nomisSequence: Int,
)
