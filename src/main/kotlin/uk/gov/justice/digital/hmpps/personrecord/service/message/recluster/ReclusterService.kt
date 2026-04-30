package uk.gov.justice.digital.hmpps.personrecord.service.message.recluster

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid.IsClusterValidResponse.Companion.result
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReviewRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.BROKEN_CLUSTER
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.OVERRIDE_CONFLICT
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster.BrokenCluster
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster.OverrideConflict
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster.SelfHealed
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchResult
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class ReclusterService(
  private val personMatchService: PersonMatchService,
  private val personKeyRepository: PersonKeyRepository,
  private val publisher: ApplicationEventPublisher,
  private val personRepository: PersonRepository,
  private val reviewRepository: ReviewRepository,
) {

  fun recluster(changedRecord: PersonEntity) {
    val cluster = changedRecord.personKey!!
    when {
      cluster.clusterIsBrokenAndCanBecomeActive() -> cluster.selfHeal()
    }
    when {
      cluster.isActive() -> processRecluster(cluster, changedRecord)
    }
  }

  private fun processRecluster(cluster: PersonKeyEntity, changedPerson: PersonEntity) {
    val matchesToChangedRecord: List<PersonMatchResult> =
      personMatchService.findPersonRecordsAboveFractureThresholdByMatchWeightDesc(changedPerson)
    val clusterDetails = ClusterDetails(cluster, changedPerson, matchesToChangedRecord)
    when {
      clusterDetails.relationship.isDifferent() -> handleClusterChange(clusterDetails)
    }
  }

  private fun handleClusterChange(clusterDetails: ClusterDetails) {
    when {
      clusterDetails.relationship.isSmaller() -> handleUnmatchedPersons(clusterDetails)
      else -> migratePersonsToMatchingCluster(clusterDetails)
    }
  }

  private fun handleUnmatchedPersons(clusterDetails: ClusterDetails) {
    when {
      clusterDetails.relationship.notMatchedToAnyRecord() -> handleInvalidClusterComposition(clusterDetails)
      else -> personMatchService.examineIsClusterValid(clusterDetails.cluster).result(
        isValid = { migratePersonsToMatchingCluster(clusterDetails) },
        isNotValid = { handleInvalidClusterComposition(clusterDetails) },
      )
    }
  }

  private fun migratePersonsToMatchingCluster(clusterDetails: ClusterDetails) {
    val matchedRecordsClusters: List<PersonKeyEntity> =
      clusterDetails.shouldJoinRecords
        .collectDistinctClusters()
        .removeUpdatedCluster(clusterDetails.cluster)
        .getActiveClusters()
    when {
      matchedRecordsClusters.isNotEmpty() -> checkCombinedClustersIsAValidClusterAndMigrate(matchedRecordsClusters, clusterDetails)
    }
  }

  private fun checkCombinedClustersIsAValidClusterAndMigrate(matchedRecordsClusters: List<PersonKeyEntity>, clusterDetails: ClusterDetails) {
    personMatchService.examineIsClusterMergeValid(clusterDetails.cluster, matchedRecordsClusters).result(
      isValid = {
        matchedRecordsClusters.forEach {
          migratePersonRecordsToMatchingCluster(it, clusterDetails.cluster)
          deleteOriginalCluster(it)
        }
      },
      isNotValid = { handleExclusionsBetweenMatchedClusters(clusterDetails.cluster, matchedRecordsClusters) },
    )
  }

  private fun handleInvalidClusterComposition(clusterDetails: ClusterDetails) {
    clusterDetails.cluster.setToNeedsAttention(BROKEN_CLUSTER)
    publisher.publishEvent(BrokenCluster(clusterDetails.cluster))
  }

  private fun handleExclusionsBetweenMatchedClusters(cluster: PersonKeyEntity, matchedRecords: List<PersonKeyEntity>) {
    cluster.setToNeedsAttention(OVERRIDE_CONFLICT)
    publisher.publishEvent(OverrideConflict(cluster, matchedRecords))
  }

  private fun migratePersonRecordsToMatchingCluster(fromCluster: PersonKeyEntity, toCluster: PersonKeyEntity) {
    val personsToMigrate = fromCluster.personEntities
    personsToMigrate.forEach { personEntity ->
      personEntity.assignToPersonKey(toCluster)
      publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECLUSTER_RECORD_MERGED, personEntity))
    }
    personRepository.saveAll(personsToMigrate)

    val fromClusterUUID = fromCluster.personUUID
    publisher.publishEvent(
      RecordTelemetry(
        TelemetryEventType.CPR_RECLUSTER_MERGE,
        mapOf(
          EventKeys.FROM_UUID to fromClusterUUID.toString(),
          EventKeys.TO_UUID to toCluster.personUUID.toString(),
        ),
      ),
    )
  }

  private fun deleteOriginalCluster(originalCluster: PersonKeyEntity) {
    originalCluster.personEntities.clear()
    val findByClustersPersonKey = reviewRepository.findByClustersPersonKey(originalCluster)
    findByClustersPersonKey?.let { review ->
      reviewRepository.delete(review)
    }
    personKeyRepository.delete(originalCluster)
  }

  private fun PersonKeyEntity.setToNeedsAttention(reason: UUIDStatusReasonType) {
    this.setAsNeedsAttention(reason)
    personKeyRepository.save(this)
  }

  private fun PersonKeyEntity.selfHeal() {
    this.setAsActive()
    personKeyRepository.save(this)
    publisher.publishEvent(SelfHealed(this))
  }

  private fun PersonKeyEntity.clusterIsBrokenAndCanBecomeActive() = this.isNotOverrideConflict() && this.clusterIsValid()

  private fun PersonKeyEntity.clusterIsValid() = if (this.hasOneRecord()) true else personMatchService.examineIsClusterValid(this).isClusterValid

  private fun List<PersonKeyEntity>.removeUpdatedCluster(cluster: PersonKeyEntity) = this.filterNot { it.id == cluster.id }

  private fun List<PersonKeyEntity>.getActiveClusters() = this.filter { it.status == ACTIVE }

  private fun List<PersonEntity>.collectDistinctClusters(): List<PersonKeyEntity> = this.groupBy { it.personKey!! }.map { it.key }.distinctBy { it.id }
}
