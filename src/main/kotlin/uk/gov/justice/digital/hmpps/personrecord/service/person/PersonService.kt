package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.exists
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonService(
  private val personRepository: PersonRepository,
  private val personKeyService: PersonKeyService,
  private val personMatchService: PersonMatchService,
) {

  fun createPersonEntity(person: Person): PersonEntity {
    val personEntity = createNewPersonEntity(person)
    personMatchService.saveToPersonMatch(personEntity)
    return personEntity
  }

  fun updatePersonEntity(person: Person, existingPersonEntity: PersonEntity): PersonUpdated {
    val oldMatchingDetails = PersonMatchRecord.from(existingPersonEntity)
    val updatedEntity = updateExistingPersonEntity(person, existingPersonEntity)
    val matchingFieldsHaveChanged = oldMatchingDetails.matchingFieldsAreDifferent(
      PersonMatchRecord.from(
        updatedEntity,
      ),
    )
    when {
      matchingFieldsHaveChanged -> personMatchService.saveToPersonMatch(updatedEntity)
    }
    return PersonUpdated(updatedEntity, matchingFieldsHaveChanged)
  }

  fun hasClusterSetBackToActive(updatedEntity: PersonEntity) = when (personKeyService.clusterNeedsAttentionAndIsInvalid(updatedEntity.personKey)) {
    true -> false
    else -> {
      personKeyService.settingNeedsAttentionClusterToActive(updatedEntity.personKey, updatedEntity)
      true
    }
  }

  fun linkRecordToPersonKey(personEntity: PersonEntity): PersonEntity {
    val personEntityWithKey = personMatchService.findHighestConfidencePersonRecord(personEntity).exists(
      no = { personKeyService.createPersonKey(personEntity) },
      yes = { personKeyService.retrievePersonKey(personEntity, it) },
    )
    return personRepository.saveAndFlush(personEntityWithKey)
  }

  private fun updateExistingPersonEntity(person: Person, personEntity: PersonEntity): PersonEntity {
    personEntity.update(person)
    return personRepository.save(personEntity)
  }

  private fun createNewPersonEntity(person: Person): PersonEntity {
    val personEntity = PersonEntity.new(person)
    return personRepository.save(personEntity)
  }
}
