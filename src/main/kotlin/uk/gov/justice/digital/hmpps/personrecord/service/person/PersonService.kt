package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonService(
  private val personRepository: PersonRepository,
  private val personKeyService: PersonKeyService,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
) {

  fun createPersonEntity(person: Person): PersonEntity {
    val personEntity = createNewPersonEntity(person)
    personMatchService.saveToPersonMatch(personEntity)
    if (person.linkOnCreate) {
      linkRecordToPersonKey(personEntity)
    }
    return personEntity
  }

  fun updatePersonEntity(person: Person, personEntity: PersonEntity): PersonUpdated {
    val oldMatchingDetails = PersonMatchRecord.from(personEntity)
    updateExistingPersonEntity(person, personEntity)
    val matchingFieldsHaveChanged = oldMatchingDetails.matchingFieldsAreDifferent(
      PersonMatchRecord.from(
        personEntity,
      ),
    )
    when {
      matchingFieldsHaveChanged -> personMatchService.saveToPersonMatch(personEntity)
    }
    personEntity.personKey?.let {
      if (person.reclusterOnUpdate && matchingFieldsHaveChanged) {
        reclusterService.recluster(personEntity)
      }
    }
    return PersonUpdated(personEntity, matchingFieldsHaveChanged)
  }

  fun linkRecordToPersonKey(personEntity: PersonEntity): PersonEntity {
    val matches = personMatchService.findHighestMatchThatPersonRecordCanJoin(personEntity)
    if (matches.isEmpty()) {
      personKeyService.assignPersonToNewPersonKey(personEntity)
    } else {
      personKeyService.assignToPersonKeyOfHighestConfidencePerson(personEntity, matches.first().personEntity)
      if (matches.size > 1) {
        reclusterService.recluster(personEntity)
      }
    }

    return personRepository.saveAndFlush(personEntity)
  }

  private fun updateExistingPersonEntity(person: Person, personEntity: PersonEntity) {
    personEntity.update(person)
    personRepository.save(personEntity)
  }

  private fun createNewPersonEntity(person: Person): PersonEntity {
    val personEntity = PersonEntity.new(person)
    return personRepository.save(personEntity)
  }
}
