package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity

data class CanonicalAlias(
  val firstName: String? = null,
  val lastName: String? = null,
  val middleNames: String? = null,
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
