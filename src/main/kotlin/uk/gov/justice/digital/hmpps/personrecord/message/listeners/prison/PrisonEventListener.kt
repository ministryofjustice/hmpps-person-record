package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.getPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues

@Component
@ConditionalOnProperty(
  name = ["sqs.listeners.enabled"],
  havingValue = "true",
  matchIfMissing = true
)
class PrisonEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val prisonEventProcessor: PrisonEventProcessor,
  private val prisonerSearchClient: PrisonerSearchClient,
) {

  @SqsListener(Queues.PRISON_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.processDomainEvent(rawMessage) {
    prisonerSearchClient.getPrisoner(it.getPrisonNumber())?.let { person ->
      prisonEventProcessor.processEvent(person)
    }
  }
}
