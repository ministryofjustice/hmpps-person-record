package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import java.time.LocalDateTime

@Schema(description = "A prisoner reference")
data class PrisonReference(
  @Schema(description = "The pseudonym id", example = "4f2b1d8a-9f3c-4a1f-8b2d-112233445566")
  val cprPseudonymId: String,
  @Schema(description = "The reference id", example = "3e2b1d8a-9f3c-4a1f-8b2d-112233445566")
  val cprReferenceId: String,
  @Schema(description = "Type of reference", example = "PNC")
  val type: IdentifierType,
  @Schema(description = "The value of the reference", example = "1231/XX/121")
  val value: String,
  @Schema(description = "The prison number for this reference", example = "A1234AB")
  val prisonNumber: String,
  @Schema(description = "Comment", example = "Important Auth")
  val comment: String? = null,
  @Schema(description = "Date reference was modified", example = "2024-01-01T12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "Username of staff member that modified reference")
  val modifyUserId: String? = null,
  @Schema(description = "Date reference was created", example = "2023-01-01T12:00:00")
  val createDateTime: LocalDateTime,
  @Schema(description = "Username of staff member that added reference", example = "jdoe")
  val createUserId: String,
)
