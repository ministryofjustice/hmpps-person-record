package uk.gov.justice.digital.hmpps.personrecord.message.processors.court

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class TransactionalLibraProcessor(
  private val personRepository: PersonRepository,
  private val personService: PersonService,
) {

  @Transactional(isolation = REPEATABLE_READ)
  fun processLibraPerson(person: Person): PersonEntity = personService.processPerson(person) {
    person.cId?.let {
      personRepository.findByCId(it)
    }
  }
}
