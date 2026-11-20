package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import java.time.LocalDateTime

@Schema(description = "A prisoner identifier")
data class PrisonIdentifierResponse(
  @Schema(description = "The alias id", example = "4f2b1d8a-9f3c-4a1f-8b2d-112233445566")
  val cprAliasId: String,
  @Schema(description = "The identifier id", example = "3e2b1d8a-9f3c-4a1f-8b2d-112233445566")
  val cprIdentifierId: String,
  @Schema(description = "Type of identifier", example = "PNC")
  val type: IdentifierType,
  @Schema(description = "The value of the identifier", example = "1231/XX/121")
  val value: String,
  @Schema(description = "The prison number of the prisoner", example = "A1234AB")
  val prisonNumber: String,
  @Schema(description = "Comment", example = "Important Auth")
  val comment: String? = null,
  @Schema(description = "Date identifier was modified", example = "2024-01-01T12:00:00")
  val modifyDateTime: LocalDateTime? = null,
  @Schema(description = "Username of staff member that modified identifier")
  val modifyUserId: String? = null,
  @Schema(description = "Date identifier was created", example = "2023-01-01T12:00:00")
  val createDateTime: LocalDateTime,
  @Schema(description = "Username of staff member that added identifier", example = "jdoe")
  val createUserId: String,
)
