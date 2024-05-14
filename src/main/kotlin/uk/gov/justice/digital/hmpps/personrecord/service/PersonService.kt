package uk.gov.justice.digital.hmpps.personrecord.service

import org.slf4j.LoggerFactory
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AliasEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class PersonService(
  private val telemetryService: TelemetryService,
  private val personRepository: PersonRepository,
  private val readWriteLockService: ReadWriteLockService,
) {

  fun processMessage(person: Person, callback: () -> List<PersonEntity>?) {
    try {
      processPerson(person, callback)
    } catch (e: ObjectOptimisticLockingFailureException) {
      log.info("Locking exception: ${e.message}, retrying message")
      processMessage(person, callback)
    }
  }

  private fun processPerson(person: Person, callback: () -> List<PersonEntity>?) = readWriteLockService.withWriteLock {
    val existingPersonEntities: List<PersonEntity>? = callback()
    when {
      (existingPersonEntities.isNullOrEmpty()) -> handlePersonCreation(person)
      else -> {
        if (existingPersonEntities.size > 1) {
          trackEvent(TelemetryEventType.CPR_MULTIPLE_RECORDS_FOUND, person)
        }
        handlePersonUpdate(person, existingPersonEntities[0])
      }
    }
  }

  private fun handlePersonCreation(person: Person) {
    updateAndSavePersonEntity(person, PersonEntity.from(person))
    trackEvent(TelemetryEventType.CPR_RECORD_CREATED, person)
  }

  private fun handlePersonUpdate(person: Person, existingPersonEntity: PersonEntity) {
    updateExistingPersonEntity(person, existingPersonEntity)
    trackEvent(TelemetryEventType.CPR_RECORD_UPDATED, person)
  }

  private fun updateExistingPersonEntity(person: Person, personEntity: PersonEntity): PersonEntity {
    var updatedPersonEntity = personEntity.update(person)
    updatedPersonEntity = removeAllChildEntities(updatedPersonEntity)
    updatePersonAliases(person, personEntity)
    return updateAndSavePersonEntity(person, updatedPersonEntity)
  }

  private fun removeAllChildEntities(personEntity: PersonEntity): PersonEntity {
    personEntity.aliases.clear()
    personEntity.addresses.clear()
    personEntity.contacts.clear()
    return personRepository.saveAndFlush(personEntity)
  }

  private fun updatePersonAddresses(person: Person, personEntity: PersonEntity) {
    val personAddresses = AddressEntity.fromList(person.addresses)
    personAddresses.forEach { personAddressEntity -> personAddressEntity.person = personEntity }
    personEntity.addresses.addAll(personAddresses)
  }

  private fun updatePersonAliases(person: Person, personEntity: PersonEntity) {
    val personAliases = AliasEntity.fromList(person.aliases)
    personAliases.forEach { personAliasEntity -> personAliasEntity.person = personEntity }
    personEntity.aliases.addAll(personAliases)
  }

  private fun updatePersonContacts(person: Person, personEntity: PersonEntity) {
    val personContacts = ContactEntity.fromList(person.contacts)
    personContacts.forEach { personContactEntity -> personContactEntity.person = personEntity }
    personEntity.contacts.addAll(personContacts)
  }

  private fun updateAndSavePersonEntity(person: Person, personEntity: PersonEntity): PersonEntity {
    updatePersonAddresses(person, personEntity)
    updatePersonContacts(person, personEntity)
    return personRepository.saveAndFlush(personEntity)
  }

  private fun trackEvent(
    eventType: TelemetryEventType,
    person: Person,
    elementMap: Map<String, String?> = emptyMap(),
  ) {
    val identifierMap = mapOf(
      "SourceSystem" to person.sourceSystemType.name,
      "DefendantId" to person.defendantId,
      "CRN" to (person.otherIdentifiers?.crn ?: ""),
    )
    telemetryService.trackEvent(eventType, identifierMap + elementMap)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
