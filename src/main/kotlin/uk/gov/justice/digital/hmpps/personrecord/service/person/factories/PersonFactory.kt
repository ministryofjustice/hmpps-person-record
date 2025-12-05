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

  fun create(person: Person): PersonChainable {
    val personEntity = PersonEntity.new(person)
    return PersonChainable(
      personEntity = personRepository.save(personEntity),
      matchingFieldsChanged = true,
      linkOnCreate = person.behaviour.linkOnCreate,
    )
  }

  fun update(person: Person, personEntity: PersonEntity): PersonChainable {
    val matchingFieldsHaveChanged = personEntity.evaluateMatchingFields {
      it.update(person)
    }
    return PersonChainable(
      personEntity = personRepository.save(personEntity),
      matchingFieldsChanged = matchingFieldsHaveChanged,
      linkOnCreate = person.behaviour.linkOnCreate,
    )
  }

  private fun PersonEntity.evaluateMatchingFields(change: (PersonEntity) -> Unit): Boolean {
    val oldMatchingDetails = PersonMatchRecord.from(this)
    change(this)
    return oldMatchingDetails.matchingFieldsAreDifferent(
      PersonMatchRecord.from(
        this,
      ),
    )
  }
}
