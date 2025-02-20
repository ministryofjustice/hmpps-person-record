package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity

data class CanonicalAlias(
  val firstName: String? = "",
  val lastName: String? = "",
  val middleNames: String? = "",
  val title: String? = "",
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
