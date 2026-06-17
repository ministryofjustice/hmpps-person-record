package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPrisonerCreatedUpdated
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues

@Component
class PrisonEventListener(
  private val prisonEventProcessor: PrisonEventProcessor,
  private val prisonerSearchClient: PrisonerSearchClient,
  private val domainEventProcessor: DomainEventProcessor,
) {

  @SqsListener(Queues.PRISON_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.process(rawMessage) { event ->
    prisonerSearchClient.getPrisoner((event as PrisonPrisonerCreatedUpdated).prisonNumber)?.let { person ->
      prisonEventProcessor.processEvent(person)
    }
  }
}
