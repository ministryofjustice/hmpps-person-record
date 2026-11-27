package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import java.time.LocalDateTime

data class PrisonIdentifier(
  @Schema(description = "The nomis identifier id. Required on requests, always null in responses")
  val nomisIdentifierId: NomisIdentifierId? = null,
  @Schema(description = "The cpr identifier id", example = "4f2c1d8a-9e3c-4a1f-8b2d-112233445566")
  val cprIdentifierId: String? = null,
  @Schema(description = "The identifier type", example = "PNC")
  val type: IdentifierType,
  @Schema(description = "The identifier value", example = "2000/1234567A")
  val value: String,
  @Schema(description = "The identifier comment", example = "Some comment")
  val comment: String? = null,
  @Schema(description = "Verified")
  val verified: Boolean,
  @Schema(description = "The identifier create date and time", example = "2000-01-01T12:00:00")
  val createDateTime: LocalDateTime? = null,
  @Schema(description = "The identifier create user id", example = "12345")
  val createUserId: String? = null,
  @Schema(description = "The identifier modify date and time", example = "2000-01-01T12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "The identifier modify user id", example = "12345")
  val modifyUserId: String? = null,
) {
  // invalid PNC/CRO values are stored as received, rest are converted into canonical form
  fun formattedValue(): String = when (type) {
    PNC -> PNCIdentifier.from(value).pncId
    CRO -> CROIdentifier.from(value).croId
    else -> value
  }.ifEmpty { value }
}

data class NomisIdentifierId(
  @Schema(description = "The nomis offender id for this identifier", example = "10000")
  val nomisOffenderId: Long,
  @Schema(description = "The sequence index for the identifier on the offender", example = "1")
  val nomisSequence: Int,
)
