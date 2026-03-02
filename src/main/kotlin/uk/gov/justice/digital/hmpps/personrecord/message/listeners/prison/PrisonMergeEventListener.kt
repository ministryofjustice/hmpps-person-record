package uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.message.processors.prison.PrisonMergeEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues

@Component
@ConditionalOnProperty(
  name = ["sqs.listeners.enabled"],
  havingValue = "true",
  matchIfMissing = true,
)
class PrisonMergeEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val mergeEventProcessor: PrisonMergeEventProcessor,
) {

  @SqsListener(Queues.PRISON_MERGE_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.processDomainEvent(rawMessage) {
    mergeEventProcessor.processEvent(it)
  }
}
