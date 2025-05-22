package uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.PersonContext

@Component
class UpdatePersonProcessor(
  private val personRepository: PersonRepository
) {

  fun updatePersonEntity(context: PersonContext): PersonEntity? {
    // sort this out
    context.personEntity?.let {
      context.personEntity?.update(context.person)
      return personRepository.save(context.personEntity)
    }
  }
}