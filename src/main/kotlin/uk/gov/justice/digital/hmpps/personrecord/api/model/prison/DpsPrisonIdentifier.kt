package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import java.time.LocalDateTime

data class DpsPrisonIdentifier(
  @Schema(description = "The cpr identifier id", example = "4f2c1d8a-9e3c-4a1f-8b2d-112233445566")
  val cprIdentifierId: String? = null,
  @Schema(description = "The identifier type", example = "PNC")
  val type: IdentifierType,
  @Schema(description = "The identifier value", example = "2000/1234567A")
  val value: String? = null,
  @Schema(description = "The identifier comment", example = "Some comment")
  val comment: String? = null,
  @Schema(description = "Verified")
  val verified: Boolean? = null,
  @Schema(description = "The identifier create date and time", example = "2000-01-01T12:00:00")
  val createDateTime: LocalDateTime? = null,
  @Schema(description = "The identifier create user id", example = "12345")
  val createUserId: String? = null,
  @Schema(description = "The identifier modify date and time", example = "2000-01-01T12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "The identifier modify user id", example = "12345")
  val modifyUserId: String? = null,
) {
  companion object {
    fun from(referenceEntity: ReferenceEntity): DpsPrisonIdentifier = DpsPrisonIdentifier(
      cprIdentifierId = referenceEntity.updateId?.toString(),
      type = referenceEntity.identifierType,
      value = referenceEntity.identifierValue,
      comment = referenceEntity.comment,
      verified = referenceEntity.isVerified,
      createDateTime = referenceEntity.createDateTime,
      createUserId = referenceEntity.createUserId,
      modifyDateTime = referenceEntity.modifyDateTime,
      modifyUserId = referenceEntity.modifyUserId,
    )
  }
}
