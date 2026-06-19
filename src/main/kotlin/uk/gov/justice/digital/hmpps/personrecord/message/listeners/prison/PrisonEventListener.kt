package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPrisonerCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPrisonerUpdated
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
    when (event) {
      is PrisonPrisonerCreated -> prisonerSearchClient.getPrisoner(event.prisonNumber)?.let { person -> prisonEventProcessor.processEvent(person) }
      is PrisonPrisonerUpdated -> prisonerSearchClient.getPrisoner(event.prisonNumber)?.let { person -> prisonEventProcessor.processEvent(person) }
      else -> log.info("Discarding message, unexpected event: $event")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
