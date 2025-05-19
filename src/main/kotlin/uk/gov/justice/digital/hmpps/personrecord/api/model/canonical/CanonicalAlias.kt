package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity

data class CanonicalAlias(
  @Schema(description = "Person alias first name", example = "Jon")
  val firstName: String? = null,
  @Schema(description = "Person alias last name", example = "do")
  val lastName: String? = null,
  @Schema(description = "Person alias middle names", example = "Morgain")
  val middleNames: String? = null,
  @Schema(description = "Person alias title")
  val title: CanonicalTitle,
) {
  companion object {

    private fun from(pseudonymEntity: PseudonymEntity): CanonicalAlias = CanonicalAlias(
      firstName = pseudonymEntity.firstName,
      middleNames = pseudonymEntity.middleNames,
      lastName = pseudonymEntity.lastName,
      title = CanonicalTitle.from(pseudonymEntity.title),
    )

    fun from(person: PersonEntity?): List<CanonicalAlias>? = person?.getAliases()?.map { from(it) }
  }
}
