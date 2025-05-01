package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.exists
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonService(
  private val personRepository: PersonRepository,
  private val personKeyService: PersonKeyService,
  private val personMatchService: PersonMatchService,
) {

  fun createPersonEntity(person: Person): PersonEntity = personRepository.save(PersonEntity.new(person))

  fun updatePersonEntity(person: Person, existingPersonEntity: PersonEntity): PersonEntity = personRepository.save(existingPersonEntity.update(person))

  fun linkRecordToPersonKey(personEntity: PersonEntity): PersonEntity {
    val personEntityWithKey = personMatchService.findHighestConfidencePersonRecord(personEntity).exists(
      no = { personKeyService.createPersonKey(personEntity) },
      yes = { personKeyService.retrievePersonKey(personEntity, it) },
    )
    return personRepository.saveAndFlush(personEntityWithKey)
  }
}
