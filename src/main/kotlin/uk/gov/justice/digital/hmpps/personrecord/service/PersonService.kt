package uk.gov.justice.digital.hmpps.personrecord.service

import kotlinx.coroutines.runBlocking
import org.hibernate.exception.ConstraintViolationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ALIAS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_DETAILS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_NEW_RECORD_EXISTS
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UPDATE_RECORD_DOES_NOT_EXIST
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED

@Service
class PersonService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val personKeyRepository: PersonKeyRepository,
  private val readWriteLockService: ReadWriteLockService,
  private val searchService: SearchService,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  private val retryExceptions = listOf(
    ObjectOptimisticLockingFailureException::class,
    CannotAcquireLockException::class,
    JpaSystemException::class,
    JpaObjectRetrievalFailureException::class,
    DataIntegrityViolationException::class,
    ConstraintViolationException::class,
  )

  fun processMessage(person: Person, event: String? = null, callback: (Boolean) -> PersonEntity?) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, retryExceptions) {
      readWriteLockService.withWriteLock { processPerson(person, event, callback) }
    }
  }

  private fun processPerson(person: Person, event: String?, callback: (Boolean) -> PersonEntity?) {
    val (isAboveSelfMatchThreshold, selfMatchScore) = searchService.retrieveRecordSelfMatchScore(person)
    person.selfMatchScore = selfMatchScore
    person.isAboveMatchScoreThreshold = isAboveSelfMatchThreshold
    val existingPersonEntity: PersonEntity? = callback(isAboveSelfMatchThreshold)
    when {
      (existingPersonEntity == null) -> handlePersonCreation(person, event)
      else -> handlePersonUpdate(person, existingPersonEntity, event)
    }
  }

  private fun handlePersonCreation(person: Person, event: String?) {
    if (isUpdateEvent(event)) {
      trackEvent(CPR_UPDATE_RECORD_DOES_NOT_EXIST, person)
    }
    val personKey: PersonKeyEntity? = when {
      person.isAboveMatchScoreThreshold -> getPersonKey(person)
      else -> null
    }
    createPersonEntity(person, personKey)
    trackEvent(TelemetryEventType.CPR_RECORD_CREATED, person)
  }

  private fun getPersonKey(person: Person): PersonKeyEntity {
    val personEntity = searchByAllSourceSystemsAndHasUuid(person)
    return when {
      personEntity == null -> createPersonKey(person)
      else -> retrievePersonKey(person, personEntity)
    }
  }

  private fun handlePersonUpdate(person: Person, existingPersonEntity: PersonEntity, event: String?) {
    if (isCreateEvent(event)) {
      trackEvent(CPR_NEW_RECORD_EXISTS, person)
    }
    updateExistingPersonEntity(person, existingPersonEntity)
    trackEvent(TelemetryEventType.CPR_RECORD_UPDATED, person)
  }

  private fun updateExistingPersonEntity(person: Person, personEntity: PersonEntity) {
    personEntity.update(person)
    personRepository.saveAndFlush(personEntity)
  }

  private fun createPersonEntity(person: Person, personKeyEntity: PersonKeyEntity?) {
    val personEntity = PersonEntity.from(person)
    personEntity.personKey = personKeyEntity
    personRepository.saveAndFlush(personEntity)
  }

  private fun isUpdateEvent(event: String?) = listOf(
    PRISONER_UPDATED,
    OFFENDER_DETAILS_CHANGED,
    OFFENDER_ALIAS_CHANGED,
    OFFENDER_ADDRESS_CHANGED,
  ).contains(event)

  private fun isCreateEvent(event: String?) = listOf(PRISONER_CREATED, NEW_OFFENDER_CREATED).contains(event)

  private fun createPersonKey(person: Person): PersonKeyEntity {
    val personKey = PersonKeyEntity.new()
    trackEvent(
      CPR_UUID_CREATED,
      person,
      mapOf(EventKeys.UUID to personKey.personId.toString()),
    )
    return personKeyRepository.saveAndFlush(personKey)
  }

  private fun retrievePersonKey(person: Person, personEntity: PersonEntity): PersonKeyEntity {
    trackEvent(
      CPR_CANDIDATE_RECORD_FOUND_UUID,
      person,
      mapOf(EventKeys.UUID to personEntity.personKey?.personId?.toString()),
    )
    return personEntity.personKey!!
  }

  fun searchByAllSourceSystemsAndHasUuid(person: Person): PersonEntity? {
    val highConfidenceMatches: List<MatchResult> = searchService.findCandidateRecordsWithUuid(person)
    return searchService.processCandidateRecords(highConfidenceMatches)
  }

  fun searchBySourceSystem(person: Person): PersonEntity? {
    val highConfidenceMatches: List<MatchResult> = searchService.findCandidateRecordsBySourceSystem(person)
    return searchService.processCandidateRecords(highConfidenceMatches)
  }

  private fun trackEvent(
    eventType: TelemetryEventType,
    person: Person,
    elementMap: Map<EventKeys, String?> = emptyMap(),
  ) {
    val identifierMap = mapOf(
      EventKeys.SOURCE_SYSTEM to person.sourceSystemType.name,
      EventKeys.DEFENDANT_ID to person.defendantId,
      EventKeys.CRN to person.crn,
      EventKeys.PRISON_NUMBER to person.prisonNumber,
    )
    telemetryService.trackEvent(eventType, identifierMap + elementMap)
  }

  companion object {
    const val MAX_ATTEMPTS: Int = 5
  }
}
