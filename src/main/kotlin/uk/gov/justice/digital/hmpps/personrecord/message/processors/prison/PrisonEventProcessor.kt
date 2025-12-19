package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class PrisonEventProcessor(
  private val personService: PersonService,
  private val personRepository: PersonRepository,
) {

  @Transactional(isolation = REPEATABLE_READ)
  fun processEvent(person: Person) {
    val reconciledPerson = PrisonPersonReconciler.reconcile(person, personRepository.findByPrisonNumber(person.prisonNumber!!))
    personService.processPerson(reconciledPerson) {
      personRepository.findByPrisonNumber(reconciledPerson.prisonNumber!!)
    }
  }
}
