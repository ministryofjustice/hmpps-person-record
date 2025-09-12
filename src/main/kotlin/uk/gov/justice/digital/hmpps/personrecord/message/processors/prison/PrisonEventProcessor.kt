package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService

@Component
class PrisonEventProcessor(
  private val createUpdateService: CreateUpdateService,
  private val personRepository: PersonRepository,
) {

  @Transactional(isolation = REPEATABLE_READ)
  fun processEvent(person: Person) {
    createUpdateService.processPerson(person) {
      personRepository.findByPrisonNumber(person.prisonNumber!!)
    }
  }
}
