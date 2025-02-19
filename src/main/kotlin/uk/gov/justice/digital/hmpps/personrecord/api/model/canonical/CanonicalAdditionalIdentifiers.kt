package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class CanonicalAdditionalIdentifiers(
  val crns: List<String?> = emptyList(),
  val defendantIds: List<String?> = emptyList(),
  val prisonNumbers: List<String?> = emptyList(),
  val cids: List<String?> = emptyList(),
) {
  companion object {

    fun from(personKeyEntity: PersonKeyEntity): CanonicalAdditionalIdentifiers = CanonicalAdditionalIdentifiers(
      crns = personKeyEntity.personEntities.mapNotNull { it.crn },
      defendantIds = personKeyEntity.personEntities.mapNotNull { it.defendantId },
      prisonNumbers = personKeyEntity.personEntities.mapNotNull { it.prisonNumber },
      cids = personKeyEntity.personEntities.mapNotNull { it.cId },
    )
  }
}
