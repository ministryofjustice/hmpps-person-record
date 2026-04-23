package uk.gov.justice.digital.hmpps.personrecord.api.model.prison

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import java.time.LocalDate

data class PrisonAlias(
  @Schema(description = "The alias title code", example = "MR")
  val titleCode: TitleCode? = null,
  @Schema(description = "The alias first name", example = "Jon")
  val firstName: String? = null,
  @Schema(description = "The alias middle names", example = "James")
  val middleNames: String? = null,
  @Schema(description = "The alias last name", example = "Smythe")
  val lastName: String? = null,
  @Schema(description = "The alias date of birth", example = "1980-01-01")
  val dateOfBirth: LocalDate? = null,
  @Schema(description = "The alias sex code", example = "M")
  val sexCode: SexCode? = null,
  @Schema(description = "Indicates if this is a primary alias", example = "true")
  val isPrimary: Boolean? = null,
  @Schema(description = "The identifiers for the prisoner alias")
  val identifiers: List<PrisonIdentifier> = emptyList(),
)

data class PrisonIdentifier(
  @Schema(description = "The identifier type", example = "PNC")
  val type: IdentifierType,
  @Schema(description = "The identifier value", example = "2000/1234567A")
  val value: String? = null,
  @Schema(description = "The identifier comment", example = "Some comment")
  val comment: String? = null,
)
