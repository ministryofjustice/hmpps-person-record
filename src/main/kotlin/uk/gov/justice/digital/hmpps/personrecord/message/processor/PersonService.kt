package uk.gov.justice.digital.hmpps.personrecord.message.processor

import org.slf4j.LoggerFactory
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

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processPerson(person: Person) {
    if (person.otherIdentifiers?.croIdentifier?.valid == false) {
      trackEvent(TelemetryEventType.INVALID_CRO, mapOf("CRO" to person.otherIdentifiers.croIdentifier.inputCro))
    }
    val existingPersonEntity: PersonEntity? = when (person.sourceSystemType) {
      SourceSystemType.HMCTS -> person.defendantId?.let { personRepository.findByDefendantId(it) }
      SourceSystemType.NOMIS -> TODO()
      SourceSystemType.DELIUS -> TODO()
      SourceSystemType.CPR -> TODO()
    }
    if (existingPersonEntity != null) {
      updatePersonEntity(person, existingPersonEntity)
    } else {
      createPersonEntity(person)
    }
  }

  private fun createPersonEntity(person: Person): PersonEntity {
    val newPersonEntity = PersonEntity.from(person)

    val personAddresses = PersonAddressEntity.fromList(person.address)
    personAddresses.forEach{ personAddressEntity -> personAddressEntity.person = newPersonEntity }
    newPersonEntity.addresses.addAll(personAddresses)

    val personAliases = PersonAliasEntity.fromList(person.personAliases)
    personAliases.forEach { personAliasEntity -> personAliasEntity.person = newPersonEntity }
    newPersonEntity.aliases.addAll(personAliases)

    val personContacts = PersonContactEntity.fromList(person.contacts)
    personContacts.forEach {personContactEntity -> personContactEntity.person = newPersonEntity }
    newPersonEntity.contacts.addAll(personContacts)

    return personRepository.saveAndFlush(newPersonEntity)
  }

  private fun updatePersonEntity(person: Person, personEntity: PersonEntity): PersonEntity {
    // TODO() update the entity with incoming values
    val updatedPerson = PersonEntity.from(person)
    updatedPerson.id = personEntity.id
    personRepository.saveAndFlush(updatedPerson)
    return updatedPerson
  }

  private fun trackEvent(
    eventType: TelemetryEventType,
    elementMap: Map<String, String?>,
  ) {
    telemetryService.trackEvent(eventType, elementMap)
  }
}
