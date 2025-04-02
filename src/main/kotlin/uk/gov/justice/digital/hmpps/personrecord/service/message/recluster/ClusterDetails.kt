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
  private val existingRecordsInCluster = cluster.personEntities.filterNot { it.id == changedRecord.id }

  val relationship = ClusterRelationship(matchedRecords, existingRecordsInCluster)
}

class ClusterRelationship(
  matchedRecords: List<PersonEntity>,
  existingRecordsInCluster: List<PersonEntity>,
) {
  private val matchedRecordsSet = matchedRecords.map { it.id }.toSet()
  private val existingRecordsSet = existingRecordsInCluster.map { it.id }.toSet()

  fun isDifferent(): Boolean = matchedRecordsSet != existingRecordsSet
  fun isSmaller() = existingRecordsSet.subtract(matchedRecordsSet).isNotEmpty()
}
