package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonAddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonAliasEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class PersonService(
  private val personRepository: PersonRepository,
  private val telemetryService: TelemetryService,
) {

  @Transactional
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  fun processPerson(person: Person, callback: () -> PersonEntity?) {
    val existingPersonEntity: PersonEntity? = callback()
    when {
      (existingPersonEntity == null) -> handlePersonCreation(person)
      else -> handlePersonUpdate(person, existingPersonEntity)
    }
  }

  private fun handlePersonCreation(person: Person) {
    updateAndSavePersonEntity(person, PersonEntity.from(person))
    trackEvent(TelemetryEventType.CPR_RECORD_CREATED, mapOf("SourceSystem" to person.sourceSystemType.name))
  }

  private fun handlePersonUpdate(person: Person, existingPersonEntity: PersonEntity) {
    updateExistingPersonEntity(person, existingPersonEntity)
    trackEvent(TelemetryEventType.CPR_RECORD_UPDATED, mapOf("SourceSystem" to existingPersonEntity.sourceSystem.name))
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
    val personAddresses = PersonAddressEntity.fromList(person.address)
    personAddresses.forEach { personAddressEntity -> personAddressEntity.person = personEntity }
    personEntity.addresses.addAll(personAddresses)
  }

  private fun updatePersonAliases(person: Person, personEntity: PersonEntity) {
    val personAliases = PersonAliasEntity.fromList(person.personAliases)
    personAliases.forEach { personAliasEntity -> personAliasEntity.person = personEntity }
    personEntity.aliases.addAll(personAliases)
  }

  private fun updatePersonContacts(person: Person, personEntity: PersonEntity) {
    val personContacts = PersonContactEntity.fromList(person.contacts)
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
    elementMap: Map<String, String?>,
  ) {
    telemetryService.trackEvent(eventType, elementMap)
  }
}
