package uk.gov.justice.digital.hmpps.personrecord.message.processors.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import kotlin.reflect.KClass

@Component
class ProbationProcessor(
  private val personRepository: PersonRepository,
  private val personService: PersonService,
) {

  fun processProbationEvent(person: Person, childrenToIgnore: Set<KClass<*>> = emptySet()): PersonEntity {
    val offender = personRepository.findByCrn(person.crn!!)
    person.masterDefendantId = offender?.masterDefendantId
    return personService.processPerson(person) {
      personRepository.findByCrn(person.crn)
    }
  }
}
