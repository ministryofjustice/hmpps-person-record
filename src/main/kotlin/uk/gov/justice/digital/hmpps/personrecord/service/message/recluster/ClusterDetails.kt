package uk.gov.justice.digital.hmpps.personrecord.service.message.recluster

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchResult

class ClusterDetails(
  val cluster: PersonKeyEntity,
  private val changedRecord: PersonEntity,
  matchesToChangeRecord: List<PersonMatchResult>,
) {

  val matchedRecords: List<PersonEntity> = matchesToChangeRecord.map { it.personEntity }
  private val existingRecordsInCluster: List<PersonEntity> = cluster.personEntities.filterNot { it.id == changedRecord.id }

  val relationship = ClusterRelationship(matchedRecords, existingRecordsInCluster)

  inner class ClusterRelationship(
    matchedRecords: List<PersonEntity>,
    existingRecordsInCluster: List<PersonEntity>,
  ) {
    private val matchedRecordsSet = matchedRecords.asIdSet()
    private val existingRecordsSet = existingRecordsInCluster.asIdSet()

    fun isDifferent(): Boolean = matchedRecordsSet != existingRecordsSet
    fun isSmaller(): Boolean = existingRecordsSet.subtract(matchedRecordsSet).isNotEmpty()

    private fun List<PersonEntity>.asIdSet(): Set<Long?> = this.map { it.id }.toSet()
  }
}
