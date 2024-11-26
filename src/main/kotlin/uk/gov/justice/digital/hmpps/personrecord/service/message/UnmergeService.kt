package uk.gov.justice.digital.hmpps.personrecord.service.message

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.merge.UnmergeEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.shouldCreateOrUpdate
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.EventLoggingService
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class UnmergeService(
  private val telemetryService: TelemetryService,
  private val personService: PersonService,
  private val personKeyRepository: PersonKeyRepository,
  private val personRepository: PersonRepository,
  private val eventLoggingService: EventLoggingService,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  fun processUnmerge(unmergeEvent: UnmergeEvent, reactivatedPersonCallback: () -> PersonEntity?, unmergedPersonCallback: () -> PersonEntity?) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, ENTITY_RETRY_EXCEPTIONS) {
      processUnmergingOfRecords(unmergeEvent, reactivatedPersonCallback, unmergedPersonCallback)
    }
  }

  private fun processUnmergingOfRecords(unmergeEvent: UnmergeEvent, reactivatedPersonCallback: () -> PersonEntity?, unmergedPersonCallback: () -> PersonEntity?) {
    val unmergedPersonEntity = retrieveUnmergedPerson(unmergeEvent, unmergedPersonCallback)
    val reactivatedPersonEntity = retrieveReactivatedPerson(unmergeEvent, reactivatedPersonCallback)

    setClusterToNeedsAttentionIfAdditionalRecords(unmergedPersonEntity, reactivatedPersonEntity)
    unmergeRecords(unmergeEvent, reactivatedPersonEntity, unmergedPersonEntity)

    val beforeDataDTO = Person.from(unmergedPersonEntity)
    val processedDataDTO = Person.from(reactivatedPersonEntity)

    eventLoggingService.recordEventLog(
      beforePerson = beforeDataDTO,
      processedPerson = processedDataDTO,
      uuid = reactivatedPersonEntity.personKey?.personId.toString(),
      eventType = unmergeEvent.event,
    )
  }

  private fun retrieveUnmergedPerson(unmergeEvent: UnmergeEvent, unmergedPersonCallback: () -> PersonEntity?): PersonEntity {
    return unmergedPersonCallback().shouldCreateOrUpdate(
      shouldCreate = {
        val personEntity = logRecordNotFoundAndCreatePerson(unmergeEvent.unmergedRecord, UnmergeRecordType.UNMERGED)
        val linkedPersonEntity = personService.linkRecordToPersonKey(personEntity)
        return@shouldCreateOrUpdate linkedPersonEntity
      },
      shouldUpdate = {
        personService.updatePersonEntity(unmergeEvent.unmergedRecord, it)
      },
    )
  }

  private fun retrieveReactivatedPerson(unmergeEvent: UnmergeEvent, reactivatedPersonCallback: () -> PersonEntity?): PersonEntity {
    return reactivatedPersonCallback().shouldCreateOrUpdate(
      shouldCreate = {
        return@shouldCreateOrUpdate logRecordNotFoundAndCreatePerson(unmergeEvent.reactivatedRecord, UnmergeRecordType.REACTIVATED)
      },
      shouldUpdate = {
        return@shouldCreateOrUpdate personService.updatePersonEntity(unmergeEvent.reactivatedRecord, it)
      },
    )
  }

  private fun logRecordNotFoundAndCreatePerson(person: Person, recordType: UnmergeRecordType): PersonEntity {
    telemetryService.trackPersonEvent(
      TelemetryEventType.CPR_UNMERGE_RECORD_NOT_FOUND,
      person,
      mapOf(EventKeys.RECORD_TYPE to recordType.name),
    )
    return personService.createPersonEntity(person)
  }

  private fun unmergeRecords(unmergeEvent: UnmergeEvent, reactivatedPersonEntity: PersonEntity, unmergedPersonEntity: PersonEntity) {
    val (excludedReactivatedRecord, excludedUnmergedRecord) = unlinkAndAddExcludeMarkersToRecords(unmergeEvent, reactivatedPersonEntity, unmergedPersonEntity)
    val linkedReactivatedRecord = personService.linkRecordToPersonKey(excludedReactivatedRecord)
    telemetryService.trackEvent(
      TelemetryEventType.CPR_RECORD_UNMERGED,
      mapOf(
        EventKeys.REACTIVATED_UUID to linkedReactivatedRecord.personKey?.personId.toString(),
        EventKeys.UNMERGED_UUID to excludedUnmergedRecord.personKey?.personId.toString(),
        unmergeEvent.unmergedSystemId.first to unmergeEvent.unmergedSystemId.second,
        unmergeEvent.reactivatedSystemId.first to unmergeEvent.reactivatedSystemId.second,
        EventKeys.SOURCE_SYSTEM to unmergeEvent.reactivatedRecord.sourceSystemType.name,
      ),
    )
  }

  private fun unlinkAndAddExcludeMarkersToRecords(unmergeEvent: UnmergeEvent, reactivatedPersonEntity: PersonEntity, unmergedPersonEntity: PersonEntity): Pair<PersonEntity, PersonEntity> {
    reactivatedPersonEntity.personKey?.let { reactivatedPersonEntity.removePersonKeyLink() }
    removeMergeLink(unmergeEvent, reactivatedPersonEntity)
    unmergedPersonEntity.addExcludeOverrideMarker(excludeRecord = reactivatedPersonEntity)
    reactivatedPersonEntity.addExcludeOverrideMarker(excludeRecord = unmergedPersonEntity)
    // Save required before linking reactivated record to new cluster
    return Pair(
      personRepository.save(reactivatedPersonEntity),
      personRepository.save(unmergedPersonEntity),
    )
  }

  private fun setClusterToNeedsAttentionIfAdditionalRecords(unmergedPersonEntity: PersonEntity, reactivatedPersonEntity: PersonEntity) {
    when {
      clusterContainsAdditionalRecords(unmergedPersonEntity, reactivatedPersonEntity) -> {
        // Get latest person key from persistence
        val personKey = personKeyRepository.findByPersonId(unmergedPersonEntity.personKey!!.personId)
        personKey!!.status = UUIDStatusType.NEEDS_ATTENTION
        unmergedPersonEntity.personKey = personKeyRepository.save(personKey)
      }
    }
  }

  private fun removeMergeLink(unmergeEvent: UnmergeEvent, reactivatedPersonEntity: PersonEntity) {
    when {
      mergeLinkExists(reactivatedPersonEntity) -> reactivatedPersonEntity.removeMergedLink()
      else -> telemetryService.trackEvent(
        TelemetryEventType.CPR_UNMERGE_LINK_NOT_FOUND,
        mapOf(
          unmergeEvent.unmergedSystemId.first to unmergeEvent.unmergedSystemId.second,
          unmergeEvent.reactivatedSystemId.first to unmergeEvent.reactivatedSystemId.second,
          EventKeys.SOURCE_SYSTEM to reactivatedPersonEntity.sourceSystem.name,
          EventKeys.RECORD_TYPE to UnmergeRecordType.REACTIVATED.name,
        ),
      )
    }
  }

  private fun clusterContainsAdditionalRecords(unmergedPersonEntity: PersonEntity, reactivatedPersonEntity: PersonEntity): Boolean {
    val additionalRecords = unmergedPersonEntity.personKey?.let { cluster ->
      cluster.personEntities.filter {
        listOf(unmergedPersonEntity.id!!, reactivatedPersonEntity.id!!).contains(it.id).not()
      }
    }
    return (additionalRecords?.size ?: 0) > 0
  }

  private fun mergeLinkExists(personEntity: PersonEntity): Boolean = personEntity.mergedTo != null

  companion object {
    enum class UnmergeRecordType {
      REACTIVATED,
      UNMERGED,
    }
    const val MAX_ATTEMPTS: Int = 5
  }
}
