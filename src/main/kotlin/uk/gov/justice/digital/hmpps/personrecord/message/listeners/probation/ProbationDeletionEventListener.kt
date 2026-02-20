package uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.message.processors.probation.ProbationDeleteProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventProcessor
import uk.gov.justice.digital.hmpps.personrecord.service.queue.Queues

@Component
@ConditionalOnProperty(
  name = ["sqs.listeners.enabled"],
  havingValue = "true",
  matchIfMissing = true,
)
class ProbationDeletionEventListener(
  private val domainEventProcessor: DomainEventProcessor,
  private val probationDeleteProcessor: ProbationDeleteProcessor,
) {

  @SqsListener(Queues.PROBATION_DELETION_EVENT_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onDomainEvent(rawMessage: String) = domainEventProcessor.processDomainEvent(rawMessage) {
    probationDeleteProcessor.processEvent(it)
  }
}
