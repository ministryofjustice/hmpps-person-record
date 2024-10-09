package uk.gov.justice.digital.hmpps.personrecord.service

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.ENTITY_RETRY_EXCEPTIONS
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ALIAS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_DETAILS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_NEW_RECORD_EXISTS
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_SELF_MATCH
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UPDATE_RECORD_DOES_NOT_EXIST

@Service
class PersonService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val readWriteLockService: ReadWriteLockService,
  private val searchService: SearchService,
  private val matchService: MatchService,
  private val personKeyService: PersonKeyService,
  @Value("\${retry.delay}") private val retryDelay: Long,
) {

  fun processMessage(person: Person, event: String? = null, callback: (isAboveSelfMatchThreshold: Boolean) -> PersonEntity?): PersonEntity = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, ENTITY_RETRY_EXCEPTIONS) {
      readWriteLockService.withWriteLock(person.sourceSystemType) { return@withWriteLock processPerson(person, event, callback) }
    }
  }

  private fun processPerson(person: Person, event: String?, callback: (isAboveSelfMatchThreshold: Boolean) -> PersonEntity?): PersonEntity {
    processSelfMatchScore(person)
    val existingPersonEntity: PersonEntity? = callback(person.isAboveMatchScoreThreshold)
    val personEntity: PersonEntity = when {
      (existingPersonEntity == null) -> handlePersonCreation(person, event)
      else -> handlePersonUpdate(person, existingPersonEntity, event)
    }
    return personEntity
  }

  private fun processSelfMatchScore(person: Person) {
    val (isAboveSelfMatchThreshold, selfMatchScore) = matchService.getSelfMatchScore(PersonSearchCriteria.from(person))
    person.selfMatchScore = selfMatchScore
    person.isAboveMatchScoreThreshold = isAboveSelfMatchThreshold
    trackEvent(
      CPR_SELF_MATCH,
      person,
      mapOf(
        EventKeys.PROBABILITY_SCORE to selfMatchScore.toString(),
        EventKeys.IS_ABOVE_SELF_MATCH_THRESHOLD to isAboveSelfMatchThreshold.toString(),
      ),
    )
  }

  private fun handlePersonCreation(person: Person, event: String?): PersonEntity {
    if (isUpdateEvent(event)) {
      trackEvent(CPR_UPDATE_RECORD_DOES_NOT_EXIST, person)
    }
    val personEntity = createPersonEntity(person)
    val personKey: PersonKeyEntity? = when {
      person.isAboveMatchScoreThreshold -> personKeyService.getPersonKey(personEntity)
      else -> handleLowSelfMatchScore(person)
    }
    linkToPersonKey(personEntity, personKey)
    trackEvent(TelemetryEventType.CPR_RECORD_CREATED, person)
    return personEntity
  }

  private fun handleLowSelfMatchScore(person: Person): PersonKeyEntity? {
    trackEvent(
      TelemetryEventType.CPR_LOW_SELF_SCORE_NOT_CREATING_UUID,
      person,
      mapOf(EventKeys.PROBABILITY_SCORE to person.selfMatchScore.toString()),
    )
    return PersonKeyEntity.empty
  }

  private fun handlePersonUpdate(person: Person, existingPersonEntity: PersonEntity, event: String?): PersonEntity {
    if (isCreateEvent(event)) {
      trackEvent(CPR_NEW_RECORD_EXISTS, person)
    }
    val updatedEntity = updateExistingPersonEntity(person, existingPersonEntity)
    trackEvent(TelemetryEventType.CPR_RECORD_UPDATED, person)
    return updatedEntity
  }

  private fun updateExistingPersonEntity(person: Person, personEntity: PersonEntity): PersonEntity {
    personEntity.update(person)
    return personRepository.saveAndFlush(personEntity)
  }

  private fun createPersonEntity(person: Person): PersonEntity {
    val personEntity = PersonEntity.from(person)
    return personRepository.saveAndFlush(personEntity)
  }

  private fun linkToPersonKey(personEntity: PersonEntity, personKeyEntity: PersonKeyEntity?) {
    personKeyEntity.let {
      personEntity.personKey = personKeyEntity
      personRepository.saveAndFlush(personEntity)
    }
  }

  private fun isUpdateEvent(event: String?) = listOf(
    PRISONER_UPDATED,
    OFFENDER_DETAILS_CHANGED,
    OFFENDER_ALIAS_CHANGED,
    OFFENDER_ADDRESS_CHANGED,
  ).contains(event)

  private fun isCreateEvent(event: String?) = listOf(PRISONER_CREATED, NEW_OFFENDER_CREATED).contains(event)

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
