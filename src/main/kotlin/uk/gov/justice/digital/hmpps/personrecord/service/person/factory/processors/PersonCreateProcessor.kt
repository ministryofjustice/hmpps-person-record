package uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.PersonContext
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonCreateProcessor(
  private val personRepository: PersonRepository,
  private val personMatchService: PersonMatchService,
) {

  fun createPersonEntity(context: PersonContext): PersonCreateProcessor {
    val personEntity = PersonEntity.new(context.person)
    personMatchService.saveToPersonMatch(personEntity)
    context.personEntity = personRepository.save(personEntity)
    return this
  }

}