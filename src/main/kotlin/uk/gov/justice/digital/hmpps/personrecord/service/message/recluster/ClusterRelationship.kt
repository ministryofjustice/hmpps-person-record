package uk.gov.justice.digital.hmpps.personrecord.service.message.recluster

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

class ClusterRelationship(
  val matchedRecords: List<PersonEntity>,
  existingRecordsInCluster: List<PersonEntity>,
) {

  private val matchedRecordsSet = matchedRecords.map { it.id }.toSet()
  private val existingRecordsSet = existingRecordsInCluster.map { it.id }.toSet()

  fun isDifferent(): Boolean = matchedRecordsSet != existingRecordsSet

  fun clusterIsSmaller() = existingRecordsSet.subtract(matchedRecordsSet).isNotEmpty()
}
