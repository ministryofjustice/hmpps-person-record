package uk.gov.justice.digital.hmpps.personrecord.service.message.recluster

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class ClusterRelationship(
  val matchedRecords: List<PersonEntity>,
  val existingRecordsInCluster: List<PersonEntity>,
) {

  private val matchedRecordsSet = matchedRecords.map { it.id }.toSet()
  private val existingRecordsSet = existingRecordsInCluster.map { it.id }.toSet()

  fun isDistinct(): Boolean = matchedRecordsSet != existingRecordsSet

  fun recordsInClusterNotMatched() = existingRecordsSet.subtract(matchedRecordsSet)
}
