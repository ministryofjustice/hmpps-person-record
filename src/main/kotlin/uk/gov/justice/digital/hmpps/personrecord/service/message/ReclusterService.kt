package uk.gov.justice.digital.hmpps.personrecord.service.message

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.EventLoggingService
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.search.MatchService
import uk.gov.justice.digital.hmpps.personrecord.service.search.SearchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.RECLUSTER_EVENT
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MATCH_FOUND_MERGE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_NO_CHANGE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_NO_MATCH_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_UUID_MARKED_NEEDS_ATTENTION

@Component
class ReclusterService(
  private val matchService: MatchService,
  private val telemetryService: TelemetryService,
  private val searchService: SearchService,
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,
  private val eventLoggingService: EventLoggingService,
) {

  @Transactional
  fun recluster(personKeyEntity: PersonKeyEntity) {
    val beforeSnapshotPersonKey = eventLoggingService.snapshotPersonKeyEntity(personKeyEntity)
    val updatedPersonKeyEntity = handleRecluster(personKeyEntity)
    eventLoggingService.recordEventLog(
      beforePersonKey = beforeSnapshotPersonKey,
      afterPersonKey = updatedPersonKeyEntity,
      uuid = personKeyEntity.personId!!,
      eventType = RECLUSTER_EVENT,
    )
  }

  private fun handleRecluster(personKeyEntity: PersonKeyEntity) = when {
    clusterNeedsAttention(personKeyEntity) -> logNeedsAttention(personKeyEntity)
    clusterHasOneRecord(personKeyEntity) -> handleSingleRecordInCluster(personKeyEntity)
    else -> handleMultipleRecordsInCluster(personKeyEntity)
  }

  private fun handleSingleRecordInCluster(personKeyEntity: PersonKeyEntity): PersonKeyEntity {
    val record = personKeyEntity.personEntities.first()
    val highConfidenceMatches = searchService.findCandidateRecordsWithUuid(record)
      .map { it.candidateRecord }
    return when {
      highConfidenceMatches.isEmpty() -> logNoMatchFound(personKeyEntity)
      else -> handleHighConfidenceMatches(record, highConfidenceMatches)
    }
  }

  private fun handleHighConfidenceMatches(personEntity: PersonEntity, highConfidenceMatches: List<PersonEntity>): PersonKeyEntity {
    val highestConfidenceRecord = highConfidenceMatches.first()
    telemetryService.trackEvent(
      CPR_RECLUSTER_MATCH_FOUND_MERGE,
      mapOf(
        EventKeys.FROM_UUID to personEntity.personKey?.personId.toString(),
        EventKeys.TO_UUID to highestConfidenceRecord.personKey?.personId.toString(),
      ),
    )
    return mergeRecordToUUID(personEntity, highestConfidenceRecord)
  }

  private fun handleMultipleRecordsInCluster(personKeyEntity: PersonKeyEntity): PersonKeyEntity {
    val notMatchedRecords = checkClusterRecordsMatch(personKeyEntity)
    return when {
      notMatchedRecords.isEmpty() -> logNoChange(personKeyEntity)
      else -> setRecordToNeedsAttention(personKeyEntity)
    }
  }

  private fun setRecordToNeedsAttention(personKeyEntity: PersonKeyEntity): PersonKeyEntity {
    personKeyEntity.status = UUIDStatusType.NEEDS_ATTENTION
    val updatedPersonKey = personKeyRepository.save(personKeyEntity)
    telemetryService.trackEvent(
      CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
      mapOf(EventKeys.UUID to personKeyEntity.personId.toString()),
    )
    return updatedPersonKey
  }

  private fun checkClusterRecordsMatch(personKeyEntity: PersonKeyEntity): List<PersonEntity> {
    val recordsInClusterMatched: MutableList<PersonEntity> = mutableListOf()
    val recordsInClusterNotMatched: MutableList<PersonEntity> = personKeyEntity.personEntities.toMutableList()

    personKeyEntity.personEntities.forEach { personEntity ->
      when {
        recordsInClusterMatched.contains(personEntity) -> return@forEach
      }
      val matchedRecords = matchRecordAgainstCluster(personEntity, personKeyEntity.personEntities)
      when {
        matchedRecords.isNotEmpty() -> {
          addAllIfNotPresent(recordsInClusterMatched, matchedRecords + personEntity)
          removeAllIfPresent(recordsInClusterNotMatched, matchedRecords + personEntity)
        }
      }
    }

    return recordsInClusterNotMatched.toList()
  }

  private fun matchRecordAgainstCluster(recordToMatch: PersonEntity, personEntities: List<PersonEntity>): List<PersonEntity> {
    val recordsToMatch = personEntities.filterNot { it == recordToMatch }
    return matchService.findHighConfidenceMatches(recordsToMatch, PersonSearchCriteria.from(recordToMatch)).map { it.candidateRecord }
  }

  private fun mergeRecordToUUID(sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity): PersonKeyEntity {
    val sourcePersonKey = sourcePersonEntity.personKey!!
    val targetPersonKey = targetPersonEntity.personKey!!

    sourcePersonKey.mergedTo = targetPersonKey.id
    sourcePersonKey.status = UUIDStatusType.RECLUSTER_MERGE
    personKeyRepository.save(sourcePersonKey)

    sourcePersonEntity.personKey = targetPersonKey
    personRepository.save(sourcePersonEntity)

    targetPersonKey.personEntities.add(sourcePersonEntity)
    personKeyRepository.save(targetPersonKey)

    sourcePersonKey.personEntities.remove(sourcePersonEntity)
    return sourcePersonKey
  }

  private fun <T> addAllIfNotPresent(list: MutableList<T>, elements: List<T>) {
    list.addAll(elements.filterNot { list.contains(it) })
  }

  private fun <T> removeAllIfPresent(list: MutableList<T>, elements: List<T>) {
    list.removeAll(elements.filter { list.contains(it) })
  }

  private fun clusterNeedsAttention(personKeyEntity: PersonKeyEntity?) = personKeyEntity?.status == UUIDStatusType.NEEDS_ATTENTION

  private fun clusterHasOneRecord(personKeyEntity: PersonKeyEntity?) = personKeyEntity?.personEntities?.size == 1

  private fun logNeedsAttention(personKeyEntity: PersonKeyEntity): PersonKeyEntity {
    telemetryService.trackEvent(
      CPR_RECLUSTER_UUID_MARKED_NEEDS_ATTENTION,
      mapOf(EventKeys.UUID to personKeyEntity.personId.toString()),
    )
    return personKeyEntity
  }

  private fun logNoMatchFound(personKeyEntity: PersonKeyEntity): PersonKeyEntity {
    telemetryService.trackEvent(
      CPR_RECLUSTER_NO_MATCH_FOUND,
      mapOf(EventKeys.UUID to personKeyEntity.personId.toString()),
    )
    return personKeyEntity
  }

  private fun logNoChange(personKeyEntity: PersonKeyEntity): PersonKeyEntity {
    telemetryService.trackEvent(
      CPR_RECLUSTER_NO_CHANGE,
      mapOf(EventKeys.UUID to personKeyEntity.personId.toString()),
    )
    return personKeyEntity
  }
}
