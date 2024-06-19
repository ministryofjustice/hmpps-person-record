package uk.gov.justice.digital.hmpps.personrecord.service

import kotlinx.coroutines.runBlocking
import org.hibernate.exception.ConstraintViolationException
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.CRO
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.NI
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.PNC
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.SOURCE_SYSTEM
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.exactMatch
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.RetryExecutor.runWithRetry
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ADDRESS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_ALIAS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_DETAILS_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.PRISONER_UPDATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_NEW_RECORD_EXISTS
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UPDATE_RECORD_DOES_NOT_EXIST

@Service
class PersonService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val readWriteLockService: ReadWriteLockService,
  private val matchService: MatchService,
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

  fun processMessage(person: Person, event: String? = null, callback: () -> PersonEntity?) = runBlocking {
    runWithRetry(MAX_ATTEMPTS, retryDelay, retryExceptions) {
      readWriteLockService.withWriteLock { processPerson(person, event, callback) }
    }
  }

  private fun processPerson(person: Person, event: String?, callback: () -> PersonEntity?) {
    val existingPersonEntity: PersonEntity? = callback()
    when {
      (existingPersonEntity == null) -> handlePersonCreation(person, event)
      else -> handlePersonUpdate(person, existingPersonEntity, event)
    }
  }

  private fun handlePersonCreation(person: Person, event: String?) {
    if (isUpdateEvent(event)) {
      trackEvent(CPR_UPDATE_RECORD_DOES_NOT_EXIST, person)
    }
    createPersonEntity(person)
    trackEvent(TelemetryEventType.CPR_RECORD_CREATED, person)
  }

  private fun handlePersonUpdate(person: Person, existingPersonEntity: PersonEntity, event: String?) {
    if (isCreateEvent(event)) {
      trackEvent(CPR_NEW_RECORD_EXISTS, person)
    }
    updateExistingPersonEntity(person, existingPersonEntity)
    trackEvent(TelemetryEventType.CPR_RECORD_UPDATED, person)
  }

  private fun updateExistingPersonEntity(person: Person, personEntity: PersonEntity) {
    removeAllChildEntities(personEntity)
    val updatedPersonEntity = personEntity.update(person)
    personRepository.saveAndFlush(updatedPersonEntity)
  }

  private fun removeAllChildEntities(personEntity: PersonEntity): PersonEntity {
    personEntity.aliases.clear()
    personEntity.addresses.clear()
    personEntity.contacts.clear()
    return personRepository.saveAndFlush(personEntity)
  }

  private fun createPersonEntity(person: Person) {
    val personEntity = PersonEntity.from(person)
    personRepository.saveAndFlush(personEntity)
  }

  private fun isUpdateEvent(event: String?) = listOf(
    PRISONER_UPDATED,
    OFFENDER_DETAILS_CHANGED,
    OFFENDER_ALIAS_CHANGED,
    OFFENDER_ADDRESS_CHANGED,
  ).contains(event)

  private fun isCreateEvent(event: String?) = listOf(PRISONER_CREATED, NEW_OFFENDER_CREATED).contains(event)

  fun findCandidateRecords(person: Person): Page<PersonEntity> {
    val postcodeSpecifications = person.addresses.map { PersonSpecification.levenshteinPostcode(it.postcode) }

    val soundexFirstLastName = Specification.where(
      PersonSpecification.soundex(person.firstName, PersonSpecification.FIRST_NAME)
        .and(PersonSpecification.soundex(person.lastName, PersonSpecification.LAST_NAME)),
    )

    val levenshteinDob = Specification.where(PersonSpecification.levenshteinDate(person.dateOfBirth, PersonSpecification.DOB))
    val levenshteinPostcode = Specification.where(PersonSpecification.combineSpecificationsWithOr(postcodeSpecifications))

    return readWriteLockService.withReadLock {
      personRepository.findAll(
        Specification.where(
          exactMatch(person.sourceSystemType.name, SOURCE_SYSTEM).and(
            exactMatch(person.otherIdentifiers?.pncIdentifier?.toString(), PNC)
              .or(exactMatch(person.driverLicenseNumber, DRIVER_LICENSE_NUMBER))
              .or(exactMatch(person.nationalInsuranceNumber, NI))
              .or(exactMatch(person.otherIdentifiers?.croIdentifier?.toString(), CRO))
              .or(soundexFirstLastName.and(levenshteinDob.or(levenshteinPostcode))),
          ),
        ),
        Pageable.ofSize(PAGE_SIZE),
      )
    }
  }

  fun processCandidateRecords(personEntities: List<PersonEntity>, person: Person): PersonEntity? {
    val highConfidenceMatches: List<MatchResult> = matchService.findHighConfidenceMatches(personEntities, person)
    return when {
      highConfidenceMatches.size == 1 -> highConfidenceMatches.first().candidateRecord
      highConfidenceMatches.size > 1 -> {
        highConfidenceMatches.forEach { candidate ->
          telemetryService.trackEvent(
            TelemetryEventType.CPR_MATCH_PERSON_DUPLICATE,
            mapOf(
              EventKeys.SOURCE_SYSTEM to candidate.candidateRecord.sourceSystem.name,
              EventKeys.DEFENDANT_ID to candidate.candidateRecord.defendantId,
              EventKeys.CRN to (candidate.candidateRecord.crn ?: ""),
              EventKeys.PRISON_NUMBER to candidate.candidateRecord.prisonNumber,
            ),
          )
        }
        highConfidenceMatches.first().candidateRecord
      }
      else -> null
    }
  }

  private fun trackEvent(
    eventType: TelemetryEventType,
    person: Person,
    elementMap: Map<EventKeys, String?> = emptyMap(),
  ) {
    val identifierMap = mapOf(
      EventKeys.SOURCE_SYSTEM to person.sourceSystemType.name,
      EventKeys.DEFENDANT_ID to person.defendantId,
      EventKeys.CRN to (person.otherIdentifiers?.crn ?: ""),
      EventKeys.PRISON_NUMBER to person.otherIdentifiers?.prisonNumber,
    )
    telemetryService.trackEvent(eventType, identifierMap + elementMap)
  }

  companion object {
    const val MAX_ATTEMPTS: Int = 5
    const val PAGE_SIZE: Int = 50
  }
}
