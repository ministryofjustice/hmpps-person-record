package uk.gov.justice.digital.hmpps.personrecord.service.recluster

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchResult
import kotlin.collections.map

class ClusterDetails(
  val existingCluster: PersonKeyEntity,
  val changedPerson: PersonEntity,
  personsThatMatchChangeRecord: List<PersonMatchResult>,
) {

  private val personsThatMatchChangedPerson: List<PersonEntity> = personsThatMatchChangeRecord.map { it.personEntity }
  val shouldJoinRecords: List<PersonEntity> = personsThatMatchChangeRecord.filter { it.shouldJoin }.map { it.personEntity }

  private val existingPersonsInCluster: List<PersonEntity> = existingCluster.personEntities.filterNot { it.id == changedPerson.id }

  val relationship = ClusterRelationship(personsThatMatchChangedPerson, existingPersonsInCluster)

  inner class ClusterRelationship(
    personsThatMatchChangedRecord: List<PersonEntity>,
    existingPersonsInCluster: List<PersonEntity>,
  ) {
    private val personIdsThatMatchChangedRecord = personsThatMatchChangedRecord.map { it.id }.toSet()
    private val existingPersonIdsInCluster = existingPersonsInCluster.map { it.id }.toSet()

    private val recordsInTheClusterThatDoNotMatch = existingPersonIdsInCluster.subtract(personIdsThatMatchChangedRecord)

    fun doesExistingCompositionDifferFromProposed(): Boolean = personIdsThatMatchChangedRecord != existingPersonIdsInCluster
    fun isSmaller(): Boolean = recordsInTheClusterThatDoNotMatch.isNotEmpty()
    fun notMatchedToAnyRecord(): Boolean = recordsInTheClusterThatDoNotMatch.size == this@ClusterDetails.existingPersonsInCluster.size
  }
}
