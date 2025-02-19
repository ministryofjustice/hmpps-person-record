package uk.gov.justice.digital.hmpps.personrecord.api.model.canonical

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

class CanonicalAdditionalIdentifiers(
  val crns: List<String?> = emptyList(),
  val defendantIds: List<String?> = emptyList(),
  val prisonNumbers: List<String?> = emptyList(),
  val cids: List<String?> = emptyList(),
) {
  companion object {

    fun from(personKeyEntity: PersonKeyEntity): CanonicalAdditionalIdentifiers = CanonicalAdditionalIdentifiers(
      crns = personKeyEntity.personEntities.map { it.crn },
      defendantIds = personKeyEntity.personEntities.map { it.defendantId },
      prisonNumbers = personKeyEntity.personEntities.map { it.prisonNumber },
      cids = personKeyEntity.personEntities.map { it.cId },
    )
  }
}
