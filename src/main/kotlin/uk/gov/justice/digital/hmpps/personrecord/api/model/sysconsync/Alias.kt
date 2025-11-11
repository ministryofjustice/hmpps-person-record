package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class Alias(
  @Schema(description = "The alias ID", example = "234026a3-5559-785b-c218-3bbaa283c944")
  val id: String?,
  @Schema(description = "The alias title code", example = "MR")
  val titleCode: String?,
  @Schema(description = "The alias first name", example = "Jon")
  val firstName: String?,
  @Schema(description = "The alias middles name", example = "James")
  val middleNames: String?,
  @Schema(description = "The alias last name", example = "Smythe")
  val lastName: String?,
  @Schema(description = "The alias type", example = "NICK")
  val type: AliasType?,
  @Schema(description = "The alias date of birth", example = "1980-01-01")
  val dateOfBirth: LocalDate?,
  @Schema(description = "The alias sex code")
  val sexCode: String?,
)

enum class AliasType {
  A,
  CN,
  MAID,
  NICK,
}
