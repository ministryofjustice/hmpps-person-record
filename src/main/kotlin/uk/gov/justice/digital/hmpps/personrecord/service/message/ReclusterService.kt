package uk.gov.justice.digital.hmpps.personrecord.service.message

import jakarta.transaction.Transactional
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonKeyService
import uk.gov.justice.digital.hmpps.personrecord.service.search.MatchService
import uk.gov.justice.digital.hmpps.personrecord.service.search.SearchService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_MATCH_FOUND_MERGE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_NO_CHANGE
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_NO_MATCH_FOUND
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECLUSTER_UUID_MARKED_NEEDS_ATTENTION
import java.util.UUID

@Component
class ReclusterService(
  private val matchService: MatchService,
  private val telemetryService: TelemetryService,
  private val personKeyService: PersonKeyService,
  private val searchService: SearchService,
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  fun recluster(personUUID: UUID?) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, ENTITY_RETRY_EXCEPTIONS) {
      personKeyRepository.findByPersonId(personUUID)?.let {
        handleRecluster(it)
      }
    }
  }

  @Transactional
  private fun handleRecluster(personKeyEntity: PersonKeyEntity) {
    when {
      clusterNeedsAttention(personKeyEntity) -> telemetryService.trackEvent(
        CPR_RECLUSTER_UUID_MARKED_NEEDS_ATTENTION,
        mapOf(EventKeys.UUID to personKeyEntity.personId.toString()),
      )
      clusterHasOneRecord(personKeyEntity) -> handleSingleRecordInCluster(personKeyEntity)
      else -> handleMultipleRecordsInCluster(personKeyEntity)
    }
  }

  private fun handleSingleRecordInCluster(personKeyEntity: PersonKeyEntity) {
    val record = personKeyEntity.personEntities.first()
    val highConfidenceMatches = searchService.findCandidateRecordsWithUuid(record)
      .map { it.candidateRecord }
    when {
      highConfidenceMatches.isEmpty() -> telemetryService.trackEvent(
        CPR_RECLUSTER_NO_MATCH_FOUND,
        mapOf(EventKeys.UUID to personKeyEntity.personId.toString()),
      )
      else -> handleHighConfidenceMatches(record, highConfidenceMatches)
    }
  }

  private fun handleHighConfidenceMatches(personEntity: PersonEntity, highConfidenceMatches: List<PersonEntity>) {
    val highestConfidenceRecord = highConfidenceMatches.first()
    telemetryService.trackEvent(
      CPR_RECLUSTER_MATCH_FOUND_MERGE,
      mapOf(
        EventKeys.FROM_UUID to personEntity.personKey?.personId.toString(),
        EventKeys.TO_UUID to highestConfidenceRecord.personKey?.personId.toString(),
      ),
    )
    mergeRecordToUUID(personEntity, highestConfidenceRecord)
  }

  private fun handleMultipleRecordsInCluster(personKeyEntity: PersonKeyEntity) {
    val notMatchedRecords = checkClusterRecordsMatch(personKeyEntity)
    when {
      notMatchedRecords.isEmpty() -> telemetryService.trackEvent(
        CPR_RECLUSTER_NO_CHANGE,
        mapOf(EventKeys.UUID to personKeyEntity.personId.toString()),
      )
      else -> {
        personKeyService.setPersonKeyStatus(personKeyEntity, UUIDStatusType.NEEDS_ATTENTION)
        telemetryService.trackEvent(
          CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
          mapOf(EventKeys.UUID to personKeyEntity.personId.toString()),
        )
      }
    }
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

  private fun mergeRecordToUUID(sourcePersonEntity: PersonEntity, targetPersonEntity: PersonEntity) {
    val sourcePersonKey = sourcePersonEntity.personKey!!
    val targetPersonKey = targetPersonEntity.personKey!!

    // Step 1: Mark the source key as merged
    sourcePersonKey.mergedTo = targetPersonKey.id
    sourcePersonKey.status = UUIDStatusType.RECLUSTER_MERGE

    // Step 2: Detach source entity from its original personKey
    sourcePersonKey.personEntities.remove(sourcePersonEntity)
    personKeyRepository.save(sourcePersonKey)

    // Step 3: Attach the source entity to the target personKey
    sourcePersonEntity.personKey = targetPersonKey
    personRepository.save(sourcePersonEntity)

    // Step 4: Add the source entity to the target's personEntities list
    targetPersonKey.personEntities.add(sourcePersonEntity)
    personKeyRepository.save(targetPersonKey)
  }

  private fun <T> addAllIfNotPresent(list: MutableList<T>, elements: List<T>) {
    list.addAll(elements.filterNot { list.contains(it) })
  }

  private fun <T> removeAllIfPresent(list: MutableList<T>, elements: List<T>) {
    list.removeAll(elements.filter { list.contains(it) })
  }

  private fun clusterNeedsAttention(personKeyEntity: PersonKeyEntity?) = personKeyEntity?.status == UUIDStatusType.NEEDS_ATTENTION

  private fun clusterHasOneRecord(personKeyEntity: PersonKeyEntity?) = personKeyEntity?.personEntities?.size == 1

  companion object {
    private const val MAX_ATTEMPTS = 5
  }
}
