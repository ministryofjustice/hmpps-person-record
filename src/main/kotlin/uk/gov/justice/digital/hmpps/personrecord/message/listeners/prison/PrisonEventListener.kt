package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.DomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.person.PrisonPersonServiceProxy
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues

@Component
class PrisonEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val personService: PrisonPersonServiceProxy,
  private val personRepository: PersonRepository,
  private val prisonerSearchClient: PrisonerSearchClient,
) {

  @SqsListener(Queues.PRISON_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.process<DomainEvent>(rawMessage) { event ->
    when (event) {
      is PrisonPersonCreated -> processPrisonEvent(event.prisonNumber)
      is PrisonPersonUpdated -> processPrisonEvent(event.prisonNumber)
      else -> log.info("Discarding message, unexpected event: $event")
    }
  }

  // TODO we now need to create a person via a different route. This route should throw
  private fun processPrisonEvent(prisonNumber: String) {
    prisonerSearchClient.getPrisoner(prisonNumber)?.let { person ->
      personService.processPerson(person) {
        personRepository.findByPrisonNumber(person.prisonNumber!!)
      }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
