package uk.gov.justice.digital.hmpps.personrecord.message.processor

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonAddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonAliasEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class PersonService(
  private val personRepository: PersonRepository,
  private val telemetryService: TelemetryService,
) {

  fun processPerson(person: Person) {
    validateCro(person)
    val existingPersonEntity: PersonEntity? = getPersonEntityBySourceSystem(person)
    handlePerson(existingPersonEntity, person)
  }

  private fun handlePerson(
    existingPersonEntity: PersonEntity?,
    person: Person,
  ) {
    if (existingPersonEntity != null) {
      updateExistingPersonEntity(person, existingPersonEntity)
      trackEvent(TelemetryEventType.CPR_RECORD_UPDATED, mapOf("SourceSystem" to existingPersonEntity.sourceSystem.name))
    } else {
      createPersonEntity(person)
      trackEvent(TelemetryEventType.CPR_RECORD_CREATED, mapOf("SourceSystem" to person.sourceSystemType.name))
    }
  }

  private fun getPersonEntityBySourceSystem(person: Person): PersonEntity? {
    val existingPersonEntity: PersonEntity? = when (person.sourceSystemType) {
      SourceSystemType.HMCTS -> person.defendantId?.let { personRepository.findByDefendantId(it) }
      SourceSystemType.NOMIS -> TODO()
      SourceSystemType.DELIUS -> TODO()
      SourceSystemType.CPR -> TODO()
    }
    return existingPersonEntity
  }

  private fun validateCro(person: Person) {
    person.otherIdentifiers?.croIdentifier?.let {
      if (!it.valid) {
        trackEvent(TelemetryEventType.INVALID_CRO, mapOf("CRO" to it.inputCro))
      }
    }
  }

  private fun createPersonEntity(person: Person): PersonEntity {
    val newPersonEntity = PersonEntity.from(person)
    return updateAndSavePersonEntity(person, newPersonEntity)
  }

  private fun updateExistingPersonEntity(person: Person, personEntity: PersonEntity): PersonEntity {
    val updatedPersonEntity = personEntity.update(person)
    return updateAndSavePersonEntity(person, updatedPersonEntity)
  }

  private fun updatePersonAddresses(person: Person, personEntity: PersonEntity) {
    personEntity.addresses.clear()
    val personAddresses = PersonAddressEntity.fromList(person.address)
    personAddresses.forEach { personAddressEntity -> personAddressEntity.person = personEntity }
    personEntity.addresses.addAll(personAddresses)
  }

  private fun updatePersonAliases(person: Person, personEntity: PersonEntity) {
    personEntity.aliases.clear()
    val personAliases = PersonAliasEntity.fromList(person.personAliases)
    personAliases.forEach { personAliasEntity -> personAliasEntity.person = personEntity }
    personEntity.aliases.addAll(personAliases)
  }

  private fun updatePersonContacts(person: Person, personEntity: PersonEntity) {
    personEntity.contacts.clear()
    val personContacts = PersonContactEntity.fromList(person.contacts)
    personContacts.forEach { personContactEntity -> personContactEntity.person = personEntity }
    personEntity.contacts.addAll(personContacts)
  }

  private fun updateAndSavePersonEntity(person: Person, personEntity: PersonEntity): PersonEntity {
    updatePersonAddresses(person, personEntity)
    updatePersonAliases(person, personEntity)
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
