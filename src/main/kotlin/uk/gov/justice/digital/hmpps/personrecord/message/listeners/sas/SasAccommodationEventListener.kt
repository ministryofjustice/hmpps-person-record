package uk.gov.justice.digital.hmpps.personrecord.message.listeners.sas

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.message.processors.sas.SasAccommodationEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues.SAS_ACCOMMODATION_EVENT_QUEUE_ID

@Component
class SasAccommodationEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val sasAccommodationEventProcessor: SasAccommodationEventProcessor,
) {

  @SqsListener(SAS_ACCOMMODATION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.processDomainEvent(rawMessage) {
    sasAccommodationEventProcessor.processEvent(it)
  }
}
