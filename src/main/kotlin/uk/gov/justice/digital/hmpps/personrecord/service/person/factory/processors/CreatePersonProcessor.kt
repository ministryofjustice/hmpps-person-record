package uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@Component
class CreatePersonProcessor(
  private val personRepository: PersonRepository,
) {

  fun createPersonEntity(person: Person): PersonEntity = personRepository.save(PersonEntity.new(person))

}