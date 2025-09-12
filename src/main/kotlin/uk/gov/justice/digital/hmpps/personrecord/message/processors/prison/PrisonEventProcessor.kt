package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService

@Component
class PrisonEventProcessor(
  private val createUpdateService: CreateUpdateService,
  private val personRepository: PersonRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
) {

  @Transactional(isolation = REPEATABLE_READ)
  fun processEvent(domainEvent: DomainEvent) {
    val prisonNumber = domainEvent.getPrisonNumber()
    prisonerSearchClient.getPrisoner(prisonNumber)?.let {
      createUpdateService.processPerson(Person.from(it)) {
        personRepository.findByPrisonNumber(prisonNumber)
      }
    }
  }
}
