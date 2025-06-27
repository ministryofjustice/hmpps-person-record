package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService

@Component
class PrisonEventProcessor(
  private val createUpdateService: CreateUpdateService,
  private val personRepository: PersonRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
) {

  fun processEvent(domainEvent: DomainEvent) {
    val prisonNumber = domainEvent.personReference?.identifiers?.first { it.type == "NOMS" }?.value!!
    prisonerSearchClient.getPrisoner(prisonNumber)?.let {
      createUpdateService.processPerson(Person.from(it)) {
        personRepository.findByPrisonNumber(prisonNumber)
      }
    }
  }
}
