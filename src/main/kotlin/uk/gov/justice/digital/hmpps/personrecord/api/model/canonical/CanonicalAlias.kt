package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity

data class CanonicalAlias(
  @Schema(description = "Person alias first name", example = "Jon")
  val firstName: String? = null,
  @Schema(description = "Person alias last name", example = "do")
  val lastName: String? = null,
  @Schema(description = "Person alias middle names", example = "Morgain")
  val middleNames: String? = null,
  @Schema(description = "Person alias title", example = "Mr")
  val title: String? = null,
) {
  companion object {

    fun from(pseudonymEntity: PseudonymEntity): CanonicalAlias = CanonicalAlias(
      firstName = pseudonymEntity.firstName,
      middleNames = pseudonymEntity.middleNames,
      lastName = pseudonymEntity.lastName,
      title = pseudonymEntity.title,
    )
    fun fromPseudonymEntityList(pseudonymEntity: List<PseudonymEntity>): List<CanonicalAlias> = pseudonymEntity.map { from(it) }
  }
}
