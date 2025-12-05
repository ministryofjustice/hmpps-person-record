package uk.gov.justice.digital.hmpps.personrecord.service.person.factories

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@Component
class PersonFactory(
  private val personRepository: PersonRepository,
) {

  fun update(person: Person, personEntity: PersonEntity): PersonChainable {
    val beforeUpdate = PersonMatchRecord.from(personEntity)
    personEntity.update(person)
    val afterUpdate = PersonMatchRecord.from(personEntity)
    return PersonChainable(
      personEntity = personRepository.save(personEntity),
      matchingFieldsChanged = beforeUpdate.matchingFieldsAreDifferent(afterUpdate),
      linkOnCreate = person.behaviour.linkOnCreate,
    )
  }
}
