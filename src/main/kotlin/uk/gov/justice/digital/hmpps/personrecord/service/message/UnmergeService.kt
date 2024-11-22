package uk.gov.justice.digital.hmpps.personrecord.service.message

import jakarta.transaction.Transactional
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.merge.UnmergeEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.shouldCreateOrUpdate
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonKeyService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class UnmergeService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val personKeyService: PersonKeyService,
  private val personService: PersonService,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  @Transactional
  fun processUnmerge(unmergeEvent: UnmergeEvent, reactivatedPersonCallback: () -> PersonEntity?, unmergedPersonCallback: () -> PersonEntity?) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, ENTITY_RETRY_EXCEPTIONS) {
      processUnmergingOfRecords(unmergeEvent, reactivatedPersonCallback, unmergedPersonCallback)
    }
  }

  private fun processUnmergingOfRecords(unmergeEvent: UnmergeEvent, reactivatedPersonCallback: () -> PersonEntity?, unmergedPersonCallback: () -> PersonEntity?) {
    val unmergedPersonEntity = retrieveUnmergedPerson(unmergeEvent, unmergedPersonCallback)
    val reactivatedPersonEntity = retrieveReactivatedPerson(unmergeEvent, reactivatedPersonCallback)
    unmergeRecords(unmergeEvent, reactivatedPersonEntity, unmergedPersonEntity)
  }

  private fun retrieveUnmergedPerson(unmergeEvent: UnmergeEvent, unmergedPersonCallback: () -> PersonEntity?): PersonEntity {
    return unmergedPersonCallback().shouldCreateOrUpdate(
      shouldCreate = {
        val personEntity = logRecordNotFoundAndCreatePerson(unmergeEvent.unmergedRecord, UnmergeRecordType.UNMERGED)
        val personKeyEntity: PersonKeyEntity = personKeyService.getPersonKey(personEntity)
        personService.linkPersonEntityToPersonKey(personEntity, personKeyEntity)
        return@shouldCreateOrUpdate personEntity
      },
      shouldUpdate = {
        personService.updatePersonEntity(unmergeEvent.unmergedRecord, it)
      }
    )
  }

  private fun retrieveReactivatedPerson(unmergeEvent: UnmergeEvent, reactivatedPersonCallback: () -> PersonEntity?): PersonEntity {
    return reactivatedPersonCallback().shouldCreateOrUpdate(
      shouldCreate = {
        return@shouldCreateOrUpdate logRecordNotFoundAndCreatePerson(unmergeEvent.reactivatedRecord, UnmergeRecordType.REACTIVATED)
      },
      shouldUpdate = {
        val updatedPersonEntity = personService.updatePersonEntity(unmergeEvent.reactivatedRecord, it)
        personService.removePersonKeyLink(updatedPersonEntity)
        return@shouldCreateOrUpdate updatedPersonEntity
      }
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
    if (clusterDoesNotHaveAdditionalRecords(unmergedPersonEntity, reactivatedPersonEntity)) {
      personKeyService.setPersonKeyStatus(unmergedPersonEntity.personKey!!, UUIDStatusType.NEEDS_ATTENTION)
    }
    removeMergeLink(unmergeEvent, reactivatedPersonEntity)
    personService.addExcludeOverrideMarker(personEntity = unmergedPersonEntity, excludingRecord = reactivatedPersonEntity)
    personService.addExcludeOverrideMarker(personEntity = reactivatedPersonEntity, excludingRecord = unmergedPersonEntity)

    // Flush changes so created records contain exclude markers to exclude from cluster results
    personRepository.saveAndFlush(unmergedPersonEntity)
    personRepository.saveAndFlush(reactivatedPersonEntity)

    findAndAssignUuid(reactivatedPersonEntity)
    telemetryService.trackEvent(
      TelemetryEventType.CPR_RECORD_UNMERGED,
      mapOf(
        EventKeys.REACTIVATED_UUID to reactivatedPersonEntity.personKey?.personId.toString(),
        EventKeys.UNMERGED_UUID to unmergedPersonEntity.personKey?.personId.toString(),
        unmergeEvent.unmergedSystemId.first to unmergeEvent.unmergedSystemId.second,
        unmergeEvent.reactivatedSystemId.first to unmergeEvent.reactivatedSystemId.second,
        EventKeys.SOURCE_SYSTEM to unmergeEvent.reactivatedRecord.sourceSystemType.name,
      ),
    )
  }

  private fun removeMergeLink(unmergeEvent: UnmergeEvent, reactivatedPersonEntity: PersonEntity) {
    when {
      mergeLinkExists(reactivatedPersonEntity) -> personService.removeMergedLink(reactivatedPersonEntity)
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

  private fun findAndAssignUuid(reactivatedPersonEntity: PersonEntity) {
    val personKeyEntity = personKeyService.getPersonKey(reactivatedPersonEntity)
    personService.linkPersonEntityToPersonKey(reactivatedPersonEntity, personKeyEntity)
  }

  private fun clusterDoesNotHaveAdditionalRecords(unmergedPersonEntity: PersonEntity, reactivatedPersonEntity: PersonEntity): Boolean {
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
